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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * An implementation of a Univeral Resource Identifier (URI). While the output
 * is mostly compatible with the Java 6 API, there are a few somewhat subtle
 * differences:
 * 
 * <pre>
 * 1) For socket related URIs, the toString() methods use semicolon (;) as the
 *    query marker instead of the normal question mark (?), and the parameters are separated
 *    with a semicolon instead of the normal ampersand (&).  With this, the URIs are compatible
 *    with those used by J2ME socket connectors.  (The Java 6 API treats socket URIs as URNs).
 * 2) This implementation does not yet "rigorously parse IPv4" addresses like the Java 6 version does,
 *    the host address is simply stored as provided by the caller.  This will be enhanced using the
 *    InetAddress class when available.
 * 3) The characters defined as legal "other characters" are not all interpreted correctly, which
 *    just means some unicode characters will get encoded that aren't required to be.  The 
 *    method URIHelper.isLegalUnicode() needs to be inspected further.
 * 4) Because of 3) toASCIIString() and toString() return the same value.
 * TODO: finish this list
 * </pre>
 * 
 * @author Eric Coolman
 * 
 * @see http://docs.oracle.com/javase/6/docs/api/java/net/URI.html
 * @see http://en.wikipedia.org/wiki/Uniform_resource_identifier
 * @see http://en.wikipedia.org/wiki/Uniform_resource_name
 * @see http://www.ietf.org/rfc/rfc2396.txt
 * @see http://www.ietf.org/rfc/rfc2732.txt
 * @see http://tools.ietf.org/html/rfc2141
 */
public class URI {
	/**
	 * Characters that are valid within a URI
	 */
	static final String UNRESERVED_EXTRAS = "_-!.~'()*";
	/**
	 * Characters that have special meaning within a URI.
	 */
	static final String PUNCTUATION = ",;:$&+=";
	/**
	 * More characters that have special meaning within a URI.
	 */
	static final String RESERVED = PUNCTUATION + "?/[]@";
	/**
	 * Character that separates the scheme from the rest of the URI.
	 */
	static final char SCHEME_SEPARATOR = ':';
	/**
	 * Character that separates the port value within an authority.
	 */
	static final char PORT_SEPARATOR = ':';
	/**
	 * Character that separates the user info value within an authority.
	 */
	static final char USERINFO_SEPARATOR = '@';
	/**
	 * Character that separates the path value from the authority
	 */
	static final char PATH_SEPARATOR = '/';
	/**
	 * A marker that identifies an authority value follows
	 */
	static final String AUTHORITY_MARKER = "//";
	/**
	 * Character that identifies an escaped octet value.
	 */
	static final char QUOTE_MARKER = '%';
	/**
	 * Character that identifies the start of a query.
	 */
	static final char QUERY_MARKER = '?';
	/**
	 * Character that separates arguments within a query
	 */
	static final char QUERY_SEPARATOR = '&';
	/**
	 * Character that separates name/value within a query parameter.
	 */
	static final char PARAMETER_SEPARATOR = '=';
	/**
	 * Character that separates arguments within a query for socket-based URLs.
	 */
	static final char SOCKET_QUERY_SEPARATOR = ';';
	/**
	 * Character that identifies the start of a fragment.
	 */
	static final char FRAGMENT_SEPARATOR = '#';
	/**
	 * The character that should be used for separating query for this URI (; or
	 * &).
	 */
	private char querySeparator;
	/**
	 * Flag that identifies if this URI has a scheme and starts with a '/'.
	 */
	private boolean opaque;
	/**
	 * Flag that identifies if this URI is relative.
	 */
	private boolean absolute;
	/**
	 * The encoded URI string value, without the scheme and fragment.
	 */
	private String schemeSpecificPart;
	/**
	 * The userinfo, host, and port segment of the URI.
	 */
	private String authority;
	/**
	 * The encoded userinfo part of the URI.
	 */
	private String userInfo;
	/**
	 * The encoded host value of the URI.
	 */
	private String host;
	/**
	 * The URI scheme, or null for opaque URIs.
	 */
	private String scheme;
	/**
	 * The encoded path value of the URI.
	 */
	private String path;
	/**
	 * The encoded query value of the URI.
	 */
	private String query;
	/**
	 * The encoded fragment value of the URI.
	 */
	private String fragment;
	/**
	 * The port value of the URI, or -1 if no port specified.
	 */
	private int port;

	/**
	 * Constructor to create a new URI object. The userInfo, path, query and
	 * fragment should be unencoded values - they will be encoded as required.
	 * 
	 * @param scheme the scheme of the URI (for URLs, this would be the
	 *            protocol), or null for relative URIs.
	 * @param userInfo the unencoded userinfo segment (ie. username:password) or
	 *            null.
	 * @param host the hostname or address, or null.
	 * @param port the host port, or -1.
	 * @param path the unencoded path segment.
	 * @param query the unencoded query segment.
	 * @param fragment the unencoded fragment (often referred to as the
	 *            'reference' or 'anchor'), or null.
	 * @throws URISyntaxException if any of the fragments are invalid.
	 */
	public URI(String scheme, String userInfo, String host, int port, String path, String query, String fragment)
			throws URISyntaxException {
		init();
		setScheme(scheme);
		setAuthority(host, port, userInfo, true);
		setPath(path, true);
		setQuery(query, true);
		setFragment(fragment, true);
	}

	/**
	 * Constructor to create a new URI object. The authority, path, query and
	 * fragment should be unencoded values - they will be encoded as required.
	 * 
	 * @param scheme the scheme of the URI (for URLs, this would be the
	 *            protocol), or null for relative URIs.
	 * @param authority the unencoded authority segment (ie.
	 *            username:password@host:port, or simply: host) or null.
	 * @param path the unencoded path segment.
	 * @param query the unencoded query segment.
	 * @param fragment the unencoded fragment (often referred to as the
	 *            'reference' or 'anchor'), or null.
	 * @throws URISyntaxException if any of the fragments are invalid.
	 */
	public URI(String scheme, String authority, String path, String query, String fragment) throws URISyntaxException {
		init();
		setScheme(scheme);
		setAuthority(authority, true);
		setPath(path, true);
		setQuery(query, true);
		setFragment(fragment, true);
	}

	/**
	 * Constructor for building URNs. The ssp and fragment should be unencoded
	 * values - they will be encoded as required.
	 * 
	 * Examples: mailto:user@codenameone.com sms:+5555551212 tel:+5555551212
	 * isbn:9781935182962
	 * 
	 * @param scheme
	 * @param ssp the unencoded scheme specific part (everything except the
	 *            scheme and fragment)
	 * @param fragment the unencoded fragment, or null
	 * @throws URISyntaxException if any of the segments are invalid.
	 */
	public URI(String scheme, String ssp, String fragment) throws URISyntaxException {
		init();
		setScheme(scheme);
		setSchemeSpecificPart(ssp, true);
		setFragment(fragment, true);
	}

	/**
	 * Constructor that parses its values from a URI string. This method expects
	 * all segments to be property encoded by the caller. The URIHelper class
	 * can be used to encode segments.
	 * 
	 * @param uriString a full encoded URI in string form to be parsed.
	 * @throws URISyntaxException if any of the parsed segments are invalid.
	 */
	public URI(String uriString) throws URISyntaxException {
		if (uriString == null) {
			throw new URISyntaxException(uriString, "Input is null");
		}
		init();
		parseURI(uriString);
	}

	/**
	 * Internal - Set the default values.
	 */
	void init() {
		absolute = true;
		querySeparator = QUERY_SEPARATOR;
		opaque = false;
		port = -1;
	}

	/**
	 * Utility method - set the scheme, ensuring valid format, and determining
	 * the query separator to use.
	 * 
	 * @see http://en.wikipedia.org/wiki/Uniform_resource_name
	 */
	protected void setScheme(String scheme) throws URISyntaxException {
		if ((this.scheme = scheme) == null) {
			absolute = false;
		} else {
			if (isValidScheme(scheme) == false) {
				throw new URISyntaxException(scheme, "Invalid scheme");
			}
			if (isSocketScheme(scheme) == true) {
				querySeparator = SOCKET_QUERY_SEPARATOR;
			}
		}
	}

	/**
	 * Utility method - set the scheme specific part, ensuring valid format. If
	 * encode=true, then some elements will be run through the encoder (path,
	 * userinfo, query, fragment), otherwise the elements will be validated for
	 * proper encoding.
	 */
	protected void setSchemeSpecificPart(String ssp, boolean encode) throws URISyntaxException {
		parseSchemeSpecificPart(ssp, true);
	}

	/**
	 * Utility method - set the part, ensuring valid format. If encode=true,
	 * then some elements will be run through the encoder (path, userinfo,
	 * query, fragment), otherwise the elements will be validated for proper
	 * encoding.
	 */
	protected void setAuthority(String newAuthority, boolean encode) throws URISyntaxException {
	}

	/**
	 * Utility method to set the query. If parameter encode=true, then the
	 * result will be encoded, otherwise the result will be validated to ensure
	 * encoding is valid. Typically the multi-parameter constructors will call
	 * this method with encode=true, and the single parameter construct will
	 * pass encode=false.
	 * 
	 * @param query
	 * @param encode
	 * @throws URISyntaxException
	 */
	protected void setQuery(String query, boolean encode) throws URISyntaxException {
	}

	/**
	 * Utility method to set the path. If parameter encode=true, then the result
	 * will be encoded, otherwise the result will be validated to ensure
	 * encoding is valid. Typically the multi-parameter constructors will call
	 * this method with encode=true, and the single parameter construct will
	 * pass encode=false.
	 * 
	 * @param path
	 * @param encode
	 * @throws URISyntaxException
	 */
	protected void setPath(String path, boolean encode) throws URISyntaxException {
	}

	/**
	 * Utility method to construct the authority segment from given host, port,
	 * and userinfo segments. If parameter encode=true, then the userinfo
	 * segment will be encoded, otherwise the it will be validated to ensure
	 * encoding is valid. Typically the multi-parameter constructors will call
	 * this method with encode=true, and the single parameter construct will
	 * pass encode=false.
	 * 
	 * @param host
	 * @param port
	 * @param userInfo
	 * @param encode
	 * @throws URISyntaxException
	 */
	protected void setAuthority(String host, int port, String userInfo, boolean encode) throws URISyntaxException {
	}

	/**
	 * Utility method to set the fragment. If parameter encode=true, then the
	 * result will be encoded, otherwise the result will be validated to ensure
	 * encoding is valid. Typically the multi-parameter constructors will call
	 * this method with encode=true, and the single parameter construct will
	 * pass encode=false.
	 * 
	 * @param fragment
	 * @param encode
	 */
	protected void setFragment(String fragment, boolean encode) {
	}

	/**
	 * Utility method to construct the scheme specific part from the uri
	 * segments (less scheme and fragment)
	 * 
	 * @return
	 */
	protected String rebuildSchemeSpecificPart() {
		StringBuffer buffer = new StringBuffer();
		if (opaque == false && (host != null || port != -1)) {
			buffer.append(AUTHORITY_MARKER);
			if (userInfo != null) {
				buffer.append(getRawUserInfo() + USERINFO_SEPARATOR);
			}
			if (host != null) {
				buffer.append(getHost());
			}
			if (port != -1) {
				buffer.append(PORT_SEPARATOR);
				buffer.append(getPort());
			}
		}
		if (path != null) {
			buffer.append(getRawPath());
		}
		if (query != null) {
			if (querySeparator == SOCKET_QUERY_SEPARATOR) {
				buffer.append(querySeparator + getRawQuery().replace(QUERY_SEPARATOR, SOCKET_QUERY_SEPARATOR));
			} else {
				buffer.append(QUERY_MARKER + getRawQuery());
			}
		}
		return buffer.toString();
	}

	/**
	 * A convenience factory method, intended to be used when the URI string is
	 * known to be valid (ie. a static application URI), so it is not needed for
	 * the caller to handle invalid syntax. NOTE: this is not away to avoid
	 * handling errors altogether - passing an invalid URI string will result in
	 * an IllegalArgumentException being thrown. The benefit here is that the
	 * compiler will not complain if you don't explicitly handle the error at
	 * compile time.
	 * 
	 * When handling a user-editable URI, use the URI constructors instead.
	 * 
	 * @param uriString URI address as a string
	 * @return parsed URI object
	 */
	public static URI create(String uriString) {
		URI uri;
		try {
			uri = new URI(uriString);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e.toString());
		}
		return uri;
	}

	/**
	 * Rather than attempting to process the uri string in a linear fashion,
	 * this implementation works its way from outside-in
	 * 
	 * @param uriString
	 * @throws URISyntaxException
	 */
	protected void parseURI(String uriString) throws URISyntaxException {
		String s = uriString;
		int index = s.indexOf(FRAGMENT_SEPARATOR);
		if (index != -1) {
			setFragment(s.substring(index + 1), false);
			s = s.substring(0, index);
		}
		index = s.indexOf(SCHEME_SEPARATOR);
		if (index != -1) {
			String scheme = s.substring(0, index);
			if (isValidScheme(scheme)) {
				setScheme(scheme);
				s = s.substring(index + 1);
			}
		}
		parseSchemeSpecificPart(s, false);
	}

	/**
	 * Utility method used to parse a given scheme specific part. If parameter
	 * encode=true, then the result will be encoded, otherwise the result will
	 * be validated to ensure encoding is valid. Typically the multi-parameter
	 * constructors will call this method with encode=true, and the single
	 * parameter construct will pass encode=false.
	 * 
	 * @param ssp scheme specific part (the URI without the scheme or fragment
	 *            included).
	 * @param encode true if ssp needs to be encoded, false if ssp needs to be
	 *            verified.
	 * @throws URISyntaxException if the ssp is invalid.
	 */
	protected void parseSchemeSpecificPart(String ssp, boolean encode) throws URISyntaxException {
		if (ssp == null) {
			throw new URISyntaxException(ssp, "Invalid scheme specific part");
		}
		if (scheme != null && ssp.charAt(0) != PATH_SEPARATOR) {
			this.opaque = true;
			this.schemeSpecificPart = ssp;
			return;
		}
		int index;
		String s = ssp;

		if ((index = s.lastIndexOf(QUERY_MARKER)) != -1) {
			setQuery(s.substring(index + 1), encode);
			s = s.substring(0, index);
		} else if (getQuerySeparator() == SOCKET_QUERY_SEPARATOR && (index = s.indexOf(SOCKET_QUERY_SEPARATOR)) != -1) {
			setQuery(s.substring(index + 1).replace(SOCKET_QUERY_SEPARATOR, QUERY_SEPARATOR), encode);
			s = s.substring(0, index);
		}

		index = (s.startsWith(AUTHORITY_MARKER)) ? 2 : 0;
		index = s.indexOf(PATH_SEPARATOR, index);
		if (index != -1) {
			setPath(s.substring(index), encode);
			s = s.substring(0, index);
		}
		setAuthority(s, encode);
	}

	/**
	 * Internal utility method to throw a syntax error if a value can not be
	 * parsed.
	 * 
	 * @param key name of the value being parsed, for error reporting.
	 * @param value value to be parsed.
	 * @return parsed integer.
	 * @throws URISyntaxException if value can not be parsed.
	 */
	int parseIntOption(String key, String value) throws URISyntaxException {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException nfe) {
			throw new URISyntaxException(value, "Invalid " + key + " value: " + value);
		}
	}

	/**
	 * Internal utility method to determine if the given scheme should use
	 * semicolons (;) for query separator instead of ampersand (&)
	 */
	boolean isSocketScheme(String scheme) {
		return false;
	}

	/**
	 * Verifies the scheme contains only valid characters as per the URN
	 * specification (see NID).
	 * 
	 * @see http://tools.ietf.org/html/rfc2141
	 */
	boolean isValidScheme(String scheme) {
		return true;
	}

	/**
	 * Get the scheme part of the URI.
	 * 
	 * @return the scheme part of the URI.
	 */
	public String getScheme() {
		return scheme;
	}

	/**
	 * Get the host name part of the URI.
	 * 
	 * @return the host name part of the URI.
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Get the port number for this URI.
	 * 
	 * @return the port number for this URI, or -1 if a port number was not
	 *         specified.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Get the decoded path part of the uri.
	 * 
	 * @return the query part of the URI, or an empty string if no path is
	 *         included in the URI.
	 */
	public String getPath() {
		return null;
	}

	/**
	 * Get the encoded path part of the uri.
	 * 
	 * @return the query part of the URI, or an empty string if no path is
	 *         included in the URI.
	 */
	public String getRawPath() {
		return path;
	}

	/**
	 * Get the decoded query part of the uri. The query marker (?) itself is not
	 * included in the result.
	 * 
	 * @return the query part of the URI.
	 */
	public String getQuery() {
		return null;
	}

	/**
	 * Get the encoded query part of the uri. The query marker (?) itself is not
	 * included in the result.
	 * 
	 * @return the query part of the URI.
	 */
	public String getRawQuery() {
		return query;
	}

	/**
	 * Get the decoded fragment (otherwise known as the &quot;reference&quot; or
	 * &quot;anchor&quot;) part of the uri. The anchor marker (#) itself is not
	 * included in the result.
	 * 
	 * @return the anchor part of the URI.
	 */
	public String getFragment() {
		return null;
	}

	/**
	 * Get the encoded fragment (otherwise known as the &quot;reference&quot; or
	 * &quot;anchor&quot;) part of the uri. The anchor marker (#) itself is not
	 * included in the result.
	 * 
	 * @return the anchor part of the URI.
	 */
	public String getRawFragment() {
		return fragment;
	}

	/**
	 * @return the schemeSpecificPart
	 */
	public String getSchemeSpecificPart() {
		return null;
	}

	/**
	 * @return the schemeSpecificPart
	 */
	public String getRawSchemeSpecificPart() {
		if (schemeSpecificPart == null) {
			schemeSpecificPart = rebuildSchemeSpecificPart();
		}
		return schemeSpecificPart;
	}

	/**
	 * @return the authority
	 */
	public String getAuthority() {
		return null;
	}

	/**
	 * @return the authority
	 */
	public String getRawAuthority() {
		return authority;
	}

	/**
	 * @return the userInfo
	 */
	public String getUserInfo() {
		return null;
	}

	/**
	 * @return the userInfo
	 */
	public String getRawUserInfo() {
		return userInfo;
	}

	/**
	 * @return true if this URI has a scheme and starts with a slash
	 */
	public boolean isOpaque() {
		return opaque;
	}

	/**
	 * @return true if the URI is not a relative URI.
	 */
	public boolean isAbsolute() {
		return absolute;
	}

	/**
	 * Get the character used for separating query. Normally this will return
	 * '?'. On J2ME Connector URLs, this method will return ';'.
	 */
	char getQuerySeparator() {
		return querySeparator;
	}

	/**
	 * @return the uri as a string
	 */
	public String toString() {
		return toASCIIString();
	}

	/**
	 * @return the uri as a string with parts encoded.
	 */
	public String toASCIIString() {
		StringBuffer buffer = new StringBuffer();
		if (scheme != null) {
			buffer.append(scheme + SCHEME_SEPARATOR);
		}
		buffer.append(getRawSchemeSpecificPart());
		if (fragment != null) {
			buffer.append(FRAGMENT_SEPARATOR + getRawFragment());
		}
		return buffer.toString();
	}

	/**
	 * Create a relative URI object against this URI, given the uri parameter.
	 * 
	 * @param uri
	 * @return
	 * @see http
	 *      ://docs.oracle.com/javase/6/docs/api/java/net/URI.html#relativize
	 *      %28java.net.URI%29
	 */
	public URI relativize(URI uri) {
		if (isOpaque() || uri.isOpaque()) {
			return uri;
		}
		if (getScheme() == null || uri.getScheme() == null || getScheme().equals(uri.getScheme()) == false) {
			return uri;
		}
		String thisAuthority = null;
		String thatAuthority = null;
		String thisPath = null;
		String thatPath = null;
		if ((thisAuthority = getAuthority()) == null || (thatAuthority = uri.getAuthority()) == null
				|| thisAuthority.equals(thatAuthority) == false) {
			return uri;
		}
		if ((thisPath = getPath()) == null || (thatPath = uri.getPath()) == null
				|| thatPath.startsWith(thisPath + PATH_SEPARATOR) == false) {
			return uri;
		}
		try {
			return new URI(null, null, thatPath.substring(thisPath.length() + 1), uri.getQuery(), uri.getFragment());
		} catch (URISyntaxException e) {
			// Since the two URIs are pre-validated, we should never get here.
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	/**
	 * Resolve a relative URI by merging it with this URI.
	 * 
	 * @param uri a URI to resolve against this URI.
	 * @return a new URI created by merging given URI with this URI.
	 * @see http
	 *      ://docs.oracle.com/javase/6/docs/api/java/net/URI.html#resolve%28
	 *      java.net.URI%29
	 */
	public URI resolve(URI uri) {
		if (isOpaque() || uri.isAbsolute()) {
			return uri;
		}
		String thatPath = uri.getPath();
		String thatQuery = uri.getQuery();
		String thatAuthority = uri.getAuthority();
		String thatFragment = uri.getFragment();
		try {
			// if standalone fragment was passed.
			if (thatFragment != null
					&& (uri.getScheme() == null && thatPath == null && thatQuery == null && thatAuthority == null)) {
				return new URI(getScheme(), getAuthority(), getPath(), getQuery(), thatFragment);
			}
			if (thatAuthority != null) {
				return new URI(getScheme(), thatAuthority, thatPath, thatQuery, thatFragment);
			}
			// an absolute path was passed
			if (thatPath != null && thatPath.charAt(0) == PATH_SEPARATOR) {
				return new URI(getScheme(), getAuthority(), thatPath, thatQuery, thatFragment);
			}
			// a relative path was passed
			String thisPath = getPath();
			if (thisPath != null) {
				int index = thisPath.lastIndexOf(PATH_SEPARATOR);
				if (index != -1) {
					thisPath = thisPath.substring(index);
				}
			}
			thisPath += PATH_SEPARATOR + thatPath;
			return new URI(getScheme(), getAuthority(), thisPath, thatQuery, thatFragment).normalize();
		} catch (URISyntaxException use) {
			// since both uri's are already validated, should never get here
			throw new IllegalArgumentException(use.getMessage());
		}

	}

	/**
	 * Normalize a URI by removing any "./" segments, and "path/../" segments.
	 * 
	 * @return a new URI instance with redundant segments removed.
	 * @see http
	 *      ://docs.oracle.com/javase/6/docs/api/java/net/URI.html#normalize%
	 *      28%29
	 */
	public URI normalize() {
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fragment == null) ? 0 : fragment.hashCode());
		result = prime * result + ((scheme == null) ? 0 : scheme.hashCode());
		result = prime * result + ((schemeSpecificPart == null) ? 0 : schemeSpecificPart.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object that) {
		if (this == that) {
			return true;
		}
		if (that == null) {
			return false;
		}
		if (getClass() != that.getClass()) {
			return false;
		}
		URI other = (URI) that;
		// compare decoded fragment
		if (fragment == null) { 
			if (other.fragment != null) {
				return false;
			}
		} else if (!getFragment().equals(other.getFragment())) {
			// the decoded value should be the same
			return false;
		}
		if (scheme == null) {
			if (other.scheme != null) {
				return false;
			}
		} else if (!scheme.equals(other.scheme)) {
			return false;
		}
		// compare decoded ssp
		if (schemeSpecificPart == null) {
			if (other.schemeSpecificPart != null) {
				return false;
			}
		} else if (!getSchemeSpecificPart().equals(other.getSchemeSpecificPart())) {
			return false;
		}
		return true;
	}
}
