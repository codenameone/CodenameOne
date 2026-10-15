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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/// java.net.URL: the same text has to come apart into the same pieces and go
/// back together into the same text.
///
/// The JDK's `equals` and `hashCode` are left out of the comparison on
/// purpose. They resolve the host name, so two names of one address are
/// equal there; JavaAPI compares the names, as `java.net.URI` does. That,
/// and the `cn1res:` protocol, are Codename One's own and run on ParparVM
/// only.
public final class URLChecks {
    /// Java 8 accepted a host name with a space in it; later JDKs refuse it.
    private static final String SPACED_HOST = "http://exa mple.com/";

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
        SPACED_HOST,
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

    private URLChecks() {
    }

    private static String describe(URL u) {
        return "protocol=" + u.getProtocol() + " host=" + u.getHost() + " port=" + u.getPort() + " default="
                + u.getDefaultPort() + " authority=" + u.getAuthority() + " userInfo=" + u.getUserInfo()
                + " path=" + u.getPath() + " query=" + u.getQuery() + " ref=" + u.getRef() + " file="
                + u.getFile() + " external=" + u.toExternalForm() + " string=" + u;
    }

    @SuppressWarnings("deprecation")
    private static String parse(String spec) {
        try {
            return describe(new URL(spec));
        } catch (MalformedURLException e) {
            return "MalformedURLException";
        }
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

    /// Runs the group.
    @SuppressWarnings("deprecation")
    public static void run() {
        Bulk.check("URL.parsesLikeTheJdk", () -> {
            for (String spec : SPECS) {
                if (!SPACED_HOST.equals(spec)) {
                    Bulk.p("[" + Bulk.esc(spec) + "]", parse(spec));
                }
            }
        });
        // The one spec on which Java 8, which the original comparison ran
        // against, and later JDKs differ, held to Java 8's answer.
        Bulk.cn1Only("URL.parsesLikeTheJdkOnJava8", () -> {
            Bulk.expectEquals("protocol=http host=exa mple.com port=-1 default=80 authority=exa mple.com"
                    + " userInfo=null path=/ query=null ref=null file=/ external=http://exa mple.com/"
                    + " string=http://exa mple.com/", parse(SPACED_HOST), SPACED_HOST);
        });
        Bulk.check("URL.resolvesRelativeReferencesLikeTheJdk", () -> {
            for (String context : CONTEXTS) {
                URL base = new URL(context);
                for (String relative : RELATIVE) {
                    String actual;
                    try {
                        actual = describe(new URL(base, relative));
                    } catch (MalformedURLException e) {
                        actual = "MalformedURLException";
                    }
                    Bulk.p("[" + context + "] + [" + relative + "]", actual);
                }
            }
            // No context is no context.
            Bulk.p("nullContext", describe(new URL(null, "http://example.com/x")));
        });
        Bulk.check("URL.buildsFromPartsLikeTheJdk", () -> {
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
                String actual;
                try {
                    actual = describe(new URL(protocol, host, port, file));
                } catch (MalformedURLException e) {
                    actual = "MalformedURLException";
                }
                Bulk.p(protocol + " " + host + " " + port + " " + file, actual);
            }
            Bulk.p("threeParts", describe(new URL("http", "example.com", "/three")));
        });
        Bulk.check("URL.convertsToUriLikeTheJdk", () -> {
            for (String spec : new String[] {"http://example.com/a?b#c", "file:/tmp/x", "https://u@h:1/p"}) {
                Bulk.p(spec, new URL(spec).toURI().toString());
            }
            Bulk.pThrown("spaced", URISyntaxException.class, () -> new URL("http://example.com/a b").toURI());
        });
        Bulk.cn1Only("URL.comparesByText", () -> {
            URL a = new URL("http://example.com/a?b#c");
            Bulk.expect(a.equals(new URL("http://example.com/a?b#c")), "equal text");
            Bulk.expect(a.hashCode() == new URL("http://example.com/a?b#c").hashCode(), "equal hash");
            // The default port is the port, and the host is not case sensitive.
            Bulk.expect(new URL("http://example.com/").equals(new URL("http://EXAMPLE.com:80/")), "default port");
            Bulk.expect(new URL("http://example.com/").hashCode() == new URL("http://EXAMPLE.com:80/").hashCode(),
                    "default port hash");
            Bulk.expect(!a.equals(new URL("http://example.com/a?b#d")), "another ref");
            Bulk.expect(!a.equals(new URL("https://example.com/a?b#c")), "another protocol");
            Bulk.expect(!a.equals(new URL("http://example.com/A?b#c")), "another path");
            Bulk.expect(!a.equals("http://example.com/a?b#c"), "a string");
            // sameFile ignores the fragment, as in the JDK.
            Bulk.expect(a.sameFile(new URL("http://example.com/a?b#other")), "sameFile");
            Bulk.expect(!a.sameFile(new URL("http://example.com/a?x#c")), "not sameFile");
        });
        Bulk.cn1Only("URL.readsApplicationResourcesThroughCn1res", () -> {
            // The form the build writes in place of Class.getResource(String).
            URL url = new URL("cn1res:/com/codename1/bulk/registered/plain.txt");
            Bulk.expectEquals("cn1res", url.getProtocol(), "protocol");
            Bulk.expectEquals("/com/codename1/bulk/registered/plain.txt", url.getPath(), "path");
            Bulk.expectEquals("", url.getHost(), "host");
            Bulk.expectEquals(Integer.valueOf(-1), Integer.valueOf(url.getPort()), "port");
            Bulk.expectEquals("cn1res:/com/codename1/bulk/registered/plain.txt", url.toExternalForm(),
                    "external");
            Bulk.expectEquals("first line\nsecond line\n", read(url.openStream()), "content");

            // A sibling resource, the way code resolves one file next to another.
            URL sibling = new URL(url, "odd-name.txt");
            Bulk.expectEquals("cn1res:/com/codename1/bulk/registered/odd-name.txt", sibling.toExternalForm(),
                    "sibling");
            Bulk.expectEquals("greeting=Registered\n", read(sibling.openStream()), "sibling content");

            // Escapes are decoded on the way to the resource name.
            Bulk.expectEquals("first line\nsecond line\n",
                    read(new URL("cn1res:/com/codename1/bulk/registered/plain%2Etxt").openStream()), "escaped");
            try {
                new URL("cn1res:/com/codename1/bulk/registered/absent.txt").openStream();
                Bulk.expect(false, "no such resource");
            } catch (FileNotFoundException e) {
                Bulk.expect(e.getMessage().indexOf("absent.txt") >= 0, "message names the resource");
            }
        });
        Bulk.cn1Only("URL.refusesProtocolsItCannotRead", () -> {
            try {
                new URL("ftp://example.com/file").openStream();
                Bulk.expect(false, "ftp is parsed, not read");
            } catch (IOException e) {
                Bulk.expect(e.getMessage().indexOf("ftp") >= 0, "message names ftp: " + e.getMessage());
            }
        });
    }
}
