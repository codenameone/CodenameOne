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

import com.codename1.compat.testing.ReferenceJdk;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Holds the `URL` shim against `java.net.URL`: the same text has to come
/// apart into the same pieces and go back together into the same text.
///
/// The JDK's `equals` and `hashCode` are left out of the comparison on
/// purpose. They resolve the host name, so two names of one address are
/// equal there; the shim compares the names, as `java.net.URI` does.
public class URLDifferentialTest {

    private static final String[] SPECS = {
        "http://example.com",
        "http://example.com/",
        "http://example.com/a/b/c.html",
        "http://example.com:8080/a?x=1&y=2#frag",
        "https://user:secret@example.com:8443/path/to?q#r",
        "https://user@example.com/",
        "HTTP://Example.COM/Path",
        "http://example.com?query",
        "http://example.com#ref",
        "http://example.com/?",
        "http://example.com/#",
        "http://example.com/a?b#c?d#e",
        "http://example.com/a%20b/c+d?e=%2F",
        "http://example.com:80/",
        "http://example.com:/",
        "http://example.com:-1/",
        "http://example.com:abc/",
        "http://example.com:99999/",
        "http://[::1]/",
        "http://[::1]:8080/x",
        "http://[2001:db8::1]/",
        "http://[::1/",
        "http://192.168.0.1:8080/",
        "http:///path",
        "http://",
        "http:",
        "http:/",
        "http:relative/path",
        "http:/absolute/path",
        "http://example.com/a/../b/./c",
        "http://example.com/..",
        "http://example.com/a/b/../../../c",
        "file:/tmp/a.txt",
        "file:///tmp/a.txt",
        "file://host/share/a.txt",
        "file:a.txt",
        "file:/C:/Users/x",
        "file:///",
        "file:",
        "file:/a b",
        "ftp://ftp.example.com/pub/file.zip",
        "ftp://anonymous:pw@ftp.example.com:2121/pub",
        "jar:file:/a/b.jar!/c/d.class",
        "jar:http://example.com/a.jar!/",
        "  http://example.com/trimmed  ",
        "url:http://example.com/prefixed",
        "URL:http://example.com/prefixed",
        "",
        "no-protocol",
        "/just/a/path",
        "://missing",
        "1http://example.com",
        "ht tp://example.com",
        "unknown://example.com/",
        "mailto:someone@example.com",
        "http://example.com/a;params?q",
        "http://example.com/a#",
        "http://exa mple.com/",
        "http://example.com/" + (char) 0xE9,
        "http://@example.com/",
        "http://user:@example.com/",
        "http://:pw@example.com/",
        "http://a@b@example.com/",
        "http://example.com/a?b?c",
        "http://example.com//double//slash",
        "http:///",
        "https://example.com:443",
        "http://example.com:0/",
        "http://example.com:+81/",
        "http://example.com:-2/",
        "http://example.com:-/",
        "http://example.com:99999999999/",
        "mailto:",
        "mailto:  ",
        "mailto:a@b.c?subject=hi#frag",
        "jar:file:/a/b.jar",
        "jar:nosuch:/a/b.jar!/c",
        "jar:file:/a/b.jar!/c/../d?q#r",
        "jar:file:/a/b.jar!/x!/y",
    };

    private static final String[] CONTEXTS = {
        "http://example.com/a/b/c?q=1#r",
        "http://example.com",
        "http://example.com/",
        "http://user@example.com:8080/dir/",
        "file:/tmp/dir/file.txt",
        "jar:file:/a/b.jar!/c/d.class",
        "https://example.com/a/b/",
        "jar:file:/a/b.jar!/",
        "mailto:x@example.com",
    };

    private static final String[] RELATIVE = {
        "d", "d/e", "/d", "../d", "../../d", "../../../d", "./d", ".", "..", "", "?x=1", "#top", "d?x#y",
        "//other.com/p", "http://absolute.com/x", "http:same/scheme", "http:/same/root", "https:other", "g;x",
        "d/./e/../f", "/..", "/./x", " spaced ", "file:/elsewhere", "ftp://f.example.com/x", "?", "#",
        "d/", "../", "./", "/", "a b", "x:y",
        "jar:file:/z.jar!/q", "#only", "d#r", "mailto:a@b",
    };

    private static String describe(java.net.URL u) {
        return "protocol=" + u.getProtocol() + " host=" + u.getHost() + " port=" + u.getPort() + " default="
                + u.getDefaultPort() + " authority=" + u.getAuthority() + " userInfo=" + u.getUserInfo()
                + " path=" + u.getPath() + " query=" + u.getQuery() + " ref=" + u.getRef() + " file="
                + u.getFile() + " external=" + u.toExternalForm() + " string=" + u;
    }

    private static String describe(URL u) {
        return "protocol=" + u.getProtocol() + " host=" + u.getHost() + " port=" + u.getPort() + " default="
                + u.getDefaultPort() + " authority=" + u.getAuthority() + " userInfo=" + u.getUserInfo()
                + " path=" + u.getPath() + " query=" + u.getQuery() + " ref=" + u.getRef() + " file="
                + u.getFile() + " external=" + u.toExternalForm() + " string=" + u;
    }

    private static String jdk(String spec) {
        try {
            return describe(new java.net.URL(spec));
        } catch (java.net.MalformedURLException e) {
            return "MalformedURLException";
        }
    }

    private static String shim(String spec) {
        try {
            return describe(new URL(spec));
        } catch (MalformedURLException e) {
            return "MalformedURLException";
        }
    }

    @Test
    public void parsesLikeTheJdk() {
        ReferenceJdk.assume("rejects a host name with a space in it");
        List<String> failures = new ArrayList<String>();
        for (String spec : SPECS) {
            String expected = jdk(spec);
            String actual = shim(spec);
            if (!expected.equals(actual)) {
                failures.add("[" + spec + "]\n  jdk =" + expected + "\n  shim=" + actual);
            }
        }
        report(failures);
    }

    @Test
    public void resolvesRelativeReferencesLikeTheJdk() throws Exception {
        List<String> failures = new ArrayList<String>();
        for (String context : CONTEXTS) {
            java.net.URL theirContext = new java.net.URL(context);
            URL myContext = new URL(context);
            for (String relative : RELATIVE) {
                String expected;
                String actual;
                try {
                    expected = describe(new java.net.URL(theirContext, relative));
                } catch (java.net.MalformedURLException e) {
                    expected = "MalformedURLException";
                }
                try {
                    actual = describe(new URL(myContext, relative));
                } catch (MalformedURLException e) {
                    actual = "MalformedURLException";
                }
                if (!expected.equals(actual)) {
                    failures.add("[" + context + "] + [" + relative + "]\n  jdk =" + expected + "\n  shim=" + actual);
                }
            }
        }
        // No context is no context.
        assertEquals(describe(new java.net.URL(null, "http://example.com/x")),
                describe(new URL(null, "http://example.com/x")));
        report(failures);
    }

    @Test
    public void buildsFromPartsLikeTheJdk() throws Exception {
        Object[][] parts = {
            {"http", "example.com", Integer.valueOf(-1), "/a?b#c"},
            {"http", "example.com", Integer.valueOf(8080), "/a"},
            {"https", "example.com", Integer.valueOf(443), ""},
            {"file", "", Integer.valueOf(-1), "/tmp/x"},
            {"file", null, Integer.valueOf(-1), "/tmp/x"},
            {"http", "::1", Integer.valueOf(80), "/v6"},
            {"http", "[::1]", Integer.valueOf(80), "/v6"},
            {"HTTP", "Example.com", Integer.valueOf(-1), "no-slash"},
            {"ftp", "example.com", Integer.valueOf(21), "/a#ref"},
            {"http", "example.com", Integer.valueOf(-2), "/bad"},
            {"nosuch", "example.com", Integer.valueOf(-1), "/bad"},
        };
        for (Object[] p : parts) {
            String protocol = (String) p[0];
            String host = (String) p[1];
            int port = ((Integer) p[2]).intValue();
            String file = (String) p[3];
            String expected;
            String actual;
            try {
                expected = describe(new java.net.URL(protocol, host, port, file));
            } catch (java.net.MalformedURLException e) {
                expected = "MalformedURLException";
            }
            try {
                actual = describe(new URL(protocol, host, port, file));
            } catch (MalformedURLException e) {
                actual = "MalformedURLException";
            }
            assertEquals(protocol + " " + host + " " + port + " " + file, expected, actual);
        }
        assertEquals(describe(new java.net.URL("http", "example.com", "/three")),
                describe(new URL("http", "example.com", "/three")));
    }

    @Test
    public void convertsToUriLikeTheJdk() throws Exception {
        for (String spec : new String[] {"http://example.com/a?b#c", "file:/tmp/x", "https://u@h:1/p"}) {
            assertEquals(new java.net.URL(spec).toURI(), new URL(spec).toURI());
        }
        try {
            new URL("http://example.com/a b").toURI();
            fail("a space is not legal in a URI");
        } catch (java.net.URISyntaxException expected) {
            // As the JDK.
        }
    }

    @Test
    public void comparesByText() throws Exception {
        URL a = new URL("http://example.com/a?b#c");
        assertEquals(a, new URL("http://example.com/a?b#c"));
        assertEquals(a.hashCode(), new URL("http://example.com/a?b#c").hashCode());
        // The default port is the port, and the host is not case sensitive.
        assertEquals(new URL("http://example.com/"), new URL("http://EXAMPLE.com:80/"));
        assertEquals(new URL("http://example.com/").hashCode(), new URL("http://EXAMPLE.com:80/").hashCode());
        assertNotEquals(a, new URL("http://example.com/a?b#d"));
        assertNotEquals(a, new URL("https://example.com/a?b#c"));
        assertNotEquals(a, new URL("http://example.com/A?b#c"));
        assertNotEquals(a, "http://example.com/a?b#c");
        // sameFile ignores the fragment, as in the JDK.
        assertTrue(a.sameFile(new URL("http://example.com/a?b#other")));
        assertFalse(a.sameFile(new URL("http://example.com/a?x#c")));
    }

    private static String read(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int b;
            while ((b = in.read()) >= 0) {
                out.write(b);
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    @Test
    public void readsApplicationResourcesThroughCn1res() throws Exception {
        // The form the build writes in place of Class.getResource(String).
        URL url = new URL("cn1res:/com/codename1/compat/jdk/registered/plain.txt");
        assertEquals("cn1res", url.getProtocol());
        assertEquals("/com/codename1/compat/jdk/registered/plain.txt", url.getPath());
        assertEquals("", url.getHost());
        assertEquals(-1, url.getPort());
        assertEquals("cn1res:/com/codename1/compat/jdk/registered/plain.txt", url.toExternalForm());
        assertEquals("first line\nsecond line\n", read(url.openStream()));

        // A sibling resource, the way code resolves one file next to another.
        URL sibling = new URL(url, "odd-name.txt");
        assertEquals("cn1res:/com/codename1/compat/jdk/registered/odd-name.txt", sibling.toExternalForm());
        assertEquals("greeting=Registered\n", read(sibling.openStream()));

        // Escapes are decoded on the way to the resource name.
        assertEquals("first line\nsecond line\n",
                read(new URL("cn1res:/com/codename1/compat/jdk/registered/plain%2Etxt").openStream()));

        try {
            new URL("cn1res:/com/codename1/compat/jdk/registered/absent.txt").openStream();
            fail("no such resource");
        } catch (FileNotFoundException e) {
            assertTrue(e.getMessage().indexOf("absent.txt") >= 0);
        }
    }

    @Test
    public void refusesProtocolsItCannotRead() throws Exception {
        try {
            new URL("ftp://example.com/file").openStream();
            fail("ftp is parsed, not read");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().indexOf("ftp") >= 0);
        }
    }

    private static void report(List<String> failures) {
        if (failures.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(failures.size()).append(" differences from the JDK:\n");
        for (int i = 0; i < failures.size() && i < 40; i++) {
            sb.append(failures.get(i)).append('\n');
        }
        fail(sb.toString());
    }
}
