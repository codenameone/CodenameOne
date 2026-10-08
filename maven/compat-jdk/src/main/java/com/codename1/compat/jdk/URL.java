/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.compat.jdk;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/// `java.net.URL` for the Codename One runtime: a parsed URL, and a way to
/// read what it points at.
///
/// Parsing follows the JDK class, including the resolution of a relative
/// reference against a context URL. The protocols accepted are `http`,
/// `https`, `file`, `ftp`, `jar`, `mailto` and `cn1res`; any other throws
/// `MalformedURLException`, as an unknown protocol does on the JDK.
///
/// #### Reading
///
/// [#openStream()] reads:
///
/// - `file:` through the application's file system (`FileInputStream` of
///   this package).
/// - `http:` and `https:` through a Codename One `ConnectionRequest`, run
///   to completion before the stream is returned: the stream is over the
///   whole downloaded body, so it suits configuration files and images, not
///   endless or very large responses.
/// - `cn1res:` from the application's packaged resources.
///
/// #### The `cn1res` scheme
///
/// `cn1res:/images/logo.png` names the resource `/images/logo.png` of the
/// application, the one `Class.getResourceAsStream("/images/logo.png")`
/// opens. The scheme exists because the device has no `Class.getResource`:
/// a later step of the build rewrites calls to `Class.getResource(String)` so
/// that they return a URL of this scheme, and code that goes on to call
/// `openStream()`, `toString()` or `toExternalForm()` on the result keeps
/// working. A `cn1res` URL has no host; its path is the absolute resource
/// name.
///
/// #### Differences from the JDK
///
/// - No `openConnection`, `getContent` or stream handler factories: the
///   device has no `URLConnection`.
/// - `equals` and `hashCode` compare host names as text, ignoring case, and
///   never resolve them.
public final class URL implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final String protocol;
    private final String host;
    private final int port;
    private final String authority;
    private final String userInfo;
    private final String path;
    private final String query;
    private final String ref;

    public URL(String spec) throws MalformedURLException {
        this(null, spec);
    }

    public URL(String protocol, String host, String file) throws MalformedURLException {
        this(protocol, host, -1, file);
    }

    public URL(String protocol, String host, int port, String file) throws MalformedURLException {
        String scheme = asciiLower(protocol);
        if (!isKnownProtocol(scheme)) {
            throw new MalformedURLException("unknown protocol: " + protocol);
        }
        this.protocol = scheme;
        if (host != null) {
            String h = host;
            if (h.indexOf(':') >= 0 && !h.startsWith("[")) {
                h = "[" + h + "]";
            }
            if (port < -1) {
                throw new MalformedURLException("Invalid port number :" + port);
            }
            this.host = h;
            this.port = port;
            this.authority = port == -1 ? h : h + ":" + port;
        } else {
            this.host = null;
            this.port = -1;
            this.authority = null;
        }
        this.userInfo = null;
        String rest = file;
        int hash = rest.indexOf('#');
        this.ref = hash < 0 ? null : rest.substring(hash + 1);
        if (hash >= 0) {
            rest = rest.substring(0, hash);
        }
        int question = rest.lastIndexOf('?');
        if (question >= 0) {
            this.query = rest.substring(question + 1);
            this.path = rest.substring(0, question);
        } else {
            this.query = null;
            this.path = rest;
        }
    }

    /// Parses `spec`, resolving it against `context` when it is a relative
    /// reference; `context` may be `null`.
    public URL(URL context, String spec) throws MalformedURLException {
        if (spec == null) {
            throw new MalformedURLException("no protocol: null");
        }
        int limit = spec.length();
        while (limit > 0 && spec.charAt(limit - 1) <= ' ') {
            limit--;
        }
        int start = 0;
        while (start < limit && spec.charAt(start) <= ' ') {
            start++;
        }
        if (spec.regionMatches(true, start, "url:", 0, 4)) {
            start += 4;
        }
        boolean isRef = start < spec.length() && spec.charAt(start) == '#';
        String newProtocol = null;
        for (int i = start; !isRef && i < limit; i++) {
            char c = spec.charAt(i);
            if (c == '/') {
                break;
            }
            if (c == ':') {
                String s = asciiLower(spec.substring(start, i));
                if (isValidProtocol(s)) {
                    newProtocol = s;
                    start = i + 1;
                }
                break;
            }
        }

        String p = newProtocol;
        String h = null;
        int prt = -1;
        String auth = null;
        String user = null;
        String pth = null;
        String qry = null;
        String rf = null;
        boolean isRelative = false;
        if (context != null && (newProtocol == null || newProtocol.equals(context.protocol))) {
            // A scheme repeated in front of a relative reference to a
            // hierarchical URL is ignored.
            if (context.path != null && context.path.startsWith("/")) {
                newProtocol = null;
            }
            if (newProtocol == null) {
                p = context.protocol;
                auth = context.authority;
                user = context.userInfo;
                h = context.host;
                prt = context.port;
                pth = context.path;
                isRelative = true;
            }
        }
        if (p == null) {
            throw new MalformedURLException("no protocol: " + spec);
        }
        if (!isKnownProtocol(p)) {
            throw new MalformedURLException("unknown protocol: " + p);
        }
        int hash = spec.indexOf('#', start);
        if (hash >= 0) {
            rf = spec.substring(hash + 1, limit);
            limit = hash;
        }
        if (isRelative && start == limit) {
            // An empty reference is the context itself.
            qry = context.query;
            if (rf == null) {
                rf = context.ref;
            }
        }

        if ("jar".equals(p) || "mailto".equals(p)) {
            // Not hierarchical: everything after the scheme is the file.
            String file;
            if ("mailto".equals(p)) {
                file = spec.substring(start, limit);
                if (file.trim().length() == 0) {
                    throw new MalformedURLException("No email address");
                }
            } else if (hash == start && isRelative) {
                file = context.getFile();
            } else {
                file = jarFile(isRelative ? context.getFile() : null, spec.substring(start, limit),
                        newProtocol != null);
            }
            if ("mailto".equals(p)) {
                // As in the JDK, an address has no fragment.
                rf = null;
            }
            int question = file.lastIndexOf('?');
            this.protocol = p;
            this.host = "";
            this.port = -1;
            this.authority = null;
            this.userInfo = null;
            this.path = question < 0 ? file : file.substring(0, question);
            this.query = question < 0 ? null : file.substring(question + 1);
            this.ref = rf;
            return;
        }

        // From here on: the authority, then the path and the query.
        String s = spec;
        boolean isRelPath = false;
        boolean queryOnly = false;
        if (start < limit) {
            int queryStart = s.indexOf('?');
            queryOnly = queryStart == start;
            if (queryStart != -1 && queryStart < limit) {
                qry = s.substring(queryStart + 1, limit);
                limit = queryStart;
                s = s.substring(0, queryStart);
            }
        }
        boolean fourSlashes = start <= limit - 4 && s.charAt(start) == '/' && s.charAt(start + 1) == '/'
                && s.charAt(start + 2) == '/' && s.charAt(start + 3) == '/';
        if (!fourSlashes && start <= limit - 2 && s.charAt(start) == '/' && s.charAt(start + 1) == '/') {
            start += 2;
            int end = s.indexOf('/', start);
            if (end < 0 || end > limit) {
                end = s.indexOf('?', start);
                if (end < 0 || end > limit) {
                    end = limit;
                }
            }
            auth = s.substring(start, end);
            h = auth;
            int at = auth.indexOf('@');
            if (at != -1) {
                if (at != auth.lastIndexOf('@')) {
                    user = null;
                    h = null;
                } else {
                    user = auth.substring(0, at);
                    h = auth.substring(at + 1);
                }
            } else {
                user = null;
            }
            prt = -1;
            if (h != null) {
                String portText = null;
                if (h.length() > 0 && h.charAt(0) == '[') {
                    int close = h.indexOf(']');
                    if (close <= 2) {
                        throw new MalformedURLException("Invalid authority field: " + auth);
                    }
                    if (h.length() > close + 1) {
                        if (h.charAt(close + 1) != ':') {
                            throw new MalformedURLException("Invalid authority field: " + auth);
                        }
                        portText = h.substring(close + 2);
                    }
                    h = h.substring(0, close + 1);
                } else {
                    int colon = h.indexOf(':');
                    if (colon >= 0) {
                        portText = h.substring(colon + 1);
                        h = h.substring(0, colon);
                    }
                }
                if (portText != null && portText.length() > 0) {
                    prt = parsePort(portText);
                }
            } else {
                h = "";
            }
            start = end;
            if (auth.length() > 0) {
                // With an authority, the path comes from the reference alone.
                pth = "";
            }
        }
        if (h == null) {
            h = "";
        }
        if (start < limit) {
            if (s.charAt(start) == '/') {
                pth = s.substring(start, limit);
            } else if (pth != null && pth.length() > 0) {
                isRelPath = true;
                int slash = pth.lastIndexOf('/');
                String separator = slash == -1 && auth != null ? "/" : "";
                pth = pth.substring(0, slash + 1) + separator + s.substring(start, limit);
            } else {
                String separator = auth != null ? "/" : "";
                pth = separator + s.substring(start, limit);
            }
        } else if (queryOnly && pth != null) {
            int slash = pth.lastIndexOf('/');
            if (slash < 0) {
                slash = 0;
            }
            pth = pth.substring(0, slash) + "/";
        }
        if (pth == null) {
            pth = "";
        }
        if (isRelPath) {
            pth = removeDots(pth);
        }
        this.protocol = p;
        this.host = h;
        this.port = prt;
        this.authority = auth;
        this.userInfo = user;
        this.path = pth;
        this.query = qry;
        this.ref = rf;
    }

    /// The index of the `/` of the last `!/` in a `jar:` file, or -1.
    private static int indexOfBangSlash(String file) {
        int bang = file.lastIndexOf('!');
        while (bang >= 0) {
            if (bang != file.length() - 1 && file.charAt(bang + 1) == '/') {
                return bang + 1;
            }
            bang = bang == 0 ? -1 : file.lastIndexOf('!', bang - 1);
        }
        return -1;
    }

    /// The file of a `jar:` URL: an archive's own URL, `!/`, and the entry.
    /// A relative reference is resolved inside the archive, never out of it.
    private static String jarFile(String contextFile, String part, boolean absolute)
            throws MalformedURLException {
        if (absolute) {
            int bangSlash = indexOfBangSlash(part);
            if (bangSlash < 0) {
                throw new MalformedURLException("no !/ in spec");
            }
            try {
                new URL(part.substring(0, bangSlash - 1));
            } catch (MalformedURLException e) {
                throw new MalformedURLException("invalid url: " + part + " (" + e.getMessage() + ")");
            }
            return part;
        }
        if (contextFile == null) {
            throw new MalformedURLException("no !/ in spec");
        }
        String file = contextFile;
        if (part.startsWith("/")) {
            int bangSlash = indexOfBangSlash(file);
            if (bangSlash < 0) {
                throw new MalformedURLException("malformed context url: no !/");
            }
            file = file.substring(0, bangSlash);
        } else if (!file.endsWith("/")) {
            int lastSlash = file.lastIndexOf('/');
            if (lastSlash < 0) {
                throw new MalformedURLException("malformed context url");
            }
            file = file.substring(0, lastSlash + 1);
        }
        file = file + part;
        int bangSlash = indexOfBangSlash(file);
        if (bangSlash < 0) {
            throw new MalformedURLException("no !/ in spec");
        }
        return file.substring(0, bangSlash) + canonizeEntry(file.substring(bangSlash));
    }

    /// Resolves `.` and `..` in the entry part of a `jar:` file.
    private static String canonizeEntry(String entry) {
        String file = entry;
        int i = file.indexOf("/../");
        while (i >= 0) {
            int lim = i > 0 ? file.lastIndexOf('/', i - 1) : -1;
            file = lim >= 0 ? file.substring(0, lim) + file.substring(i + 3) : file.substring(i + 3);
            i = file.indexOf("/../");
        }
        i = file.indexOf("/./");
        while (i >= 0) {
            file = file.substring(0, i) + file.substring(i + 2);
            i = file.indexOf("/./");
        }
        while (file.endsWith("/..")) {
            i = file.indexOf("/..");
            int lim = i > 0 ? file.lastIndexOf('/', i - 1) : -1;
            file = lim >= 0 ? file.substring(0, lim + 1) : file.substring(0, i);
        }
        if (file.endsWith("/.")) {
            file = file.substring(0, file.length() - 1);
        }
        return file;
    }

    /// A port as the JDK reads one: a decimal `int` with an optional sign,
    /// of which -1, "no port", is the only negative accepted.
    private static int parsePort(String text) throws MalformedURLException {
        int from = 0;
        boolean negative = false;
        if (text.charAt(0) == '-' || text.charAt(0) == '+') {
            negative = text.charAt(0) == '-';
            from = 1;
        }
        long value = 0;
        if (text.length() == from || text.length() - from > 10) {
            throw new MalformedURLException("For input string: \"" + text + "\"");
        }
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                throw new MalformedURLException("For input string: \"" + text + "\"");
            }
            value = value * 10 + (c - '0');
        }
        if (negative) {
            value = -value;
        }
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new MalformedURLException("For input string: \"" + text + "\"");
        }
        if (value < -1) {
            throw new MalformedURLException("Invalid port number :" + value);
        }
        return (int) value;
    }

    /// Resolves the `.` and `..` segments a relative reference brought into
    /// a path.
    private static String removeDots(String in) {
        String path = in;
        int i = path.indexOf("/./");
        while (i >= 0) {
            path = path.substring(0, i) + path.substring(i + 2);
            i = path.indexOf("/./");
        }
        i = path.indexOf("/../");
        while (i >= 0) {
            // "/a/b/../c" becomes "/a/c"; "/../../a" has nothing to cancel
            // and stays.
            int previous = i > 0 ? path.lastIndexOf('/', i - 1) : -1;
            if (previous >= 0 && path.indexOf("/../", previous) != 0) {
                path = path.substring(0, previous) + path.substring(i + 3);
                i = path.indexOf("/../");
            } else {
                i = path.indexOf("/../", i + 3);
            }
        }
        while (path.endsWith("/..")) {
            i = path.indexOf("/..");
            int previous = i > 0 ? path.lastIndexOf('/', i - 1) : -1;
            if (previous < 0) {
                break;
            }
            path = path.substring(0, previous + 1);
        }
        if (path.startsWith("./") && path.length() > 2) {
            path = path.substring(2);
        }
        if (path.endsWith("/.")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /// Lower case for the ASCII letters only. A scheme is ASCII by
    /// specification, and `String.toLowerCase()` follows the device's locale.
    private static String asciiLower(String s) {
        StringBuilder sb = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (sb == null) {
                    sb = new StringBuilder(s);
                }
                sb.setCharAt(i, (char) (c + ('a' - 'A')));
            }
        }
        return sb == null ? s : sb.toString();
    }

    private static boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isValidProtocol(String protocol) {
        int len = protocol.length();
        if (len < 1 || !isAsciiLetter(protocol.charAt(0))) {
            return false;
        }
        for (int i = 1; i < len; i++) {
            char c = protocol.charAt(i);
            boolean digit = c >= '0' && c <= '9';
            if (!isAsciiLetter(c) && !digit && c != '.' && c != '+' && c != '-') {
                return false;
            }
        }
        return true;
    }

    private static boolean isKnownProtocol(String protocol) {
        return "http".equals(protocol) || "https".equals(protocol) || "file".equals(protocol)
                || "cn1res".equals(protocol) || "ftp".equals(protocol) || "jar".equals(protocol)
                || "mailto".equals(protocol);
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getDefaultPort() {
        if ("http".equals(protocol)) {
            return 80;
        }
        if ("https".equals(protocol)) {
            return 443;
        }
        if ("ftp".equals(protocol)) {
            return 21;
        }
        return -1;
    }

    public String getAuthority() {
        return authority;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public String getPath() {
        return path;
    }

    public String getQuery() {
        return query;
    }

    public String getRef() {
        return ref;
    }

    public String getFile() {
        return query == null ? path : path + "?" + query;
    }

    public String toExternalForm() {
        StringBuilder sb = new StringBuilder();
        sb.append(protocol).append(':');
        if (authority != null && authority.length() > 0) {
            sb.append("//").append(authority);
        }
        if (path != null) {
            sb.append(path);
        }
        if (query != null) {
            sb.append('?').append(query);
        }
        if (ref != null) {
            sb.append('#').append(ref);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toExternalForm();
    }

    public URI toURI() throws URISyntaxException {
        return new URI(toString());
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private int effectivePort() {
        return port != -1 ? port : getDefaultPort();
    }

    /// Whether the two URLs name the same resource, the fragment aside.
    public boolean sameFile(URL other) {
        if (other == null) {
            return false;
        }
        boolean hostsEqual = host == null || other.host == null
                ? host == null && other.host == null
                : host.equalsIgnoreCase(other.host);
        return protocol.equals(other.protocol) && same(getFile(), other.getFile())
                && effectivePort() == other.effectivePort() && hostsEqual;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof URL)) {
            return false;
        }
        URL other = (URL) obj;
        return same(ref, other.ref) && sameFile(other);
    }

    @Override
    public int hashCode() {
        int h = protocol.hashCode();
        if (host != null) {
            h += asciiLower(host).hashCode();
        }
        String file = getFile();
        if (file != null) {
            h += file.hashCode();
        }
        h += effectivePort();
        if (ref != null) {
            h += ref.hashCode();
        }
        return h;
    }

    private static int hex(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    /// Replaces each `%XX` by the byte it stands for and reads the result
    /// as UTF-8. A `%` not followed by two hex digits is kept.
    private static String percentDecode(String s) throws IOException {
        if (s.indexOf('%') < 0) {
            return s;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int i = 0;
        while (i < s.length()) {
            if (s.charAt(i) == '%' && i + 2 < s.length() && hex(s.charAt(i + 1)) >= 0
                    && hex(s.charAt(i + 2)) >= 0) {
                bytes.write((hex(s.charAt(i + 1)) << 4) | hex(s.charAt(i + 2)));
                i += 3;
            } else {
                // Up to the next escape in one piece, so that a surrogate
                // pair is encoded as the one character it is.
                int next = s.indexOf('%', i + 1);
                if (next < 0) {
                    next = s.length();
                }
                bytes.write(s.substring(i, next).getBytes("UTF-8"));
                i = next;
            }
        }
        return new String(bytes.toByteArray(), "UTF-8");
    }

    /// Opens the resource for reading; see the class description for what
    /// each protocol reads.
    ///
    /// #### Throws
    ///
    /// - `FileNotFoundException`: if the file or resource does not exist, or
    ///   the server answers 404 or 410
    ///
    /// - `IOException`: if the request fails, the server answers another
    ///   error status, or the protocol cannot be read on the device
    public InputStream openStream() throws IOException {
        if ("file".equals(protocol)) {
            return new FileInputStream(new File(percentDecode(path)));
        }
        if ("cn1res".equals(protocol)) {
            String name = percentDecode(path);
            if (!name.startsWith("/")) {
                name = "/" + name;
            }
            InputStream in = URL.class.getResourceAsStream(name);
            if (in == null) {
                throw new FileNotFoundException(toExternalForm());
            }
            return in;
        }
        if ("http".equals(protocol) || "https".equals(protocol)) {
            return openHttp();
        }
        throw new IOException("Cannot read the " + protocol + " protocol: " + toExternalForm());
    }

    private InputStream openHttp() throws IOException {
        StringBuilder address = new StringBuilder();
        address.append(protocol).append("://").append(authority == null ? "" : authority).append(path);
        if (query != null) {
            address.append('?').append(query);
        }
        ConnectionRequest request = new ConnectionRequest();
        request.setUrl(address.toString());
        request.setPost(false);
        // Failures are reported to the caller as exceptions, not to the user
        // as the default error dialog.
        request.setFailSilently(true);
        NetworkManager.getInstance().addToQueueAndWait(request);
        int code = request.getResponseCode();
        if (code == 404 || code == 410) {
            throw new FileNotFoundException(toExternalForm());
        }
        if (code >= 400) {
            throw new IOException("Server returned HTTP response code: " + code + " for URL: " + toExternalForm());
        }
        byte[] data = request.getResponseData();
        if (data == null) {
            throw new IOException("No response from " + toExternalForm());
        }
        return new ByteArrayInputStream(data);
    }
}
