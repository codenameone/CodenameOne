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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Holds the `.properties` parser against `java.util.Properties`, and the
/// bundle lookup against `java.util.ResourceBundle` reading the same files.
public class ResourceBundleDifferentialTest {

    private static final String BASE = "com.codename1.compat.jdk.bundles.Messages";

    // A backslash, kept out of string literals next to a 'u': javac reads
    // that pair as an escape wherever it appears.
    private static final String BS = String.valueOf((char) 92);

    private Locale saved;

    @Before
    public void useUsLocale() {
        saved = Locale.getDefault();
        Locale.setDefault(Locale.US);
        ResourceBundle.clearCache();
        java.util.ResourceBundle.clearCache();
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(saved);
        ResourceBundle.clearCache();
    }

    // ------------------------------------------------------------------
    // The .properties format
    // ------------------------------------------------------------------

    private static Map<String, String> jdkParse(String text) throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(text));
        Map<String, String> out = new TreeMap<String, String>();
        for (String name : properties.stringPropertyNames()) {
            out.put(name, properties.getProperty(name));
        }
        return out;
    }

    private static Map<String, String> shimParse(String text) {
        Map<String, String> out = new HashMap<String, String>();
        PropertyResourceBundle.parse(text, out);
        return new TreeMap<String, String>(out);
    }

    private static String outcome(String text, boolean jdk) {
        try {
            return (jdk ? jdkParse(text) : shimParse(text)).toString();
        } catch (IllegalArgumentException e) {
            return "IllegalArgumentException";
        } catch (IOException e) {
            return "IOException";
        }
    }

    @Test
    public void parsesPropertiesLikeTheJdk() {
        String[] texts = {
            "",
            "a=b",
            "a=b\n",
            "a = b",
            "a : b",
            "a:b",
            "a b",
            "a\tb",
            "a   =   b   ",
            "   a=b",
            "\t a=b",
            "a",
            "a=",
            "=b",
            ":b",
            " = b",
            "# comment\na=b",
            "! comment\na=b",
            "  # indented comment\na=b",
            "a=b # not a comment",
            "a=b\r\nc=d\re=f\ng=h",
            "a=b\n\n\nc=d",
            "a=first\na=second",
            "long=one \\\n    two \\\n    three",
            "long=one\\\r\n   two",
            "odd=ends with two \\\\\nnext=1",
            "odd=ends with three \\\\\\\nstill the value",
            "key\\ with\\ spaces=value",
            "key\\=with\\:separators=value",
            "key\\",
            "a=b\\",
            "tabs=\\ta\\nb\\rc\\fd",
            "unknown=\\q\\z\\'\\\"",
            "unicode=" + BS + "u0041" + BS + "u00e9" + BS + "u4E2D",
            "unicodeKey" + BS + "u0020x=1",
            "lower=" + BS + "u00ff" + BS + "u00FF",
            "# a comment does not continue \\\na=b",
            "a=b\n   \n  \t\nc=d",
            "\\#notcomment=1",
            "\\!notcomment=1",
            "a==b",
            "a=:b",
            "a:=b",
            "a = = b",
            "a\\\n  b=c",
            "a=b\\\n",
            "a=b\\\n\n",
            "a=b\\\n# not a comment\n",
            "a=\\\n\\\n\\\nb",
            "path=c:\\\\dir\\\\file",
            "url=http://example.com/a?b=c",
            "sp\\ ace : v",
            "a\fb=c",
            "\fa=b",
            "a=" + (char) 0xE9 + (char) 0x4E2D,
            "trailing=value\t \f",
            "k1=v1\nk2:v2\nk3 v3\n",
        };
        List<String> failures = new ArrayList<String>();
        for (String text : texts) {
            String expected = outcome(text, true);
            String actual = outcome(text, false);
            if (!expected.equals(actual)) {
                failures.add(show(text) + ": jdk=" + show(expected) + " shim=" + show(actual));
            }
        }
        report(failures);
    }

    @Test
    public void rejectsMalformedEscapesLikeTheJdk() {
        String[] texts = {
            "a=" + BS + "u12", "a=" + BS + "uXYZW", "a=" + BS + "u", "a=" + BS + "u123", BS + "u00G0=1",
            "a=" + BS + "u12\nb=2",
        };
        for (String text : texts) {
            assertEquals(show(text), outcome(text, true), outcome(text, false));
        }
    }

    @Test
    public void parsesRandomPropertiesLikeTheJdk() {
        ReferenceJdk.assume("reads a comment marker that follows an empty continued line as a comment");
        String[] pieces = {
            "a", "b", "key", "value", " ", "  ", "\t", "\f", "=", ":", "#", "!", "\\", "\\\\", "\n", "\r", "\r\n",
            "\\n", "\\t", "\\ ", "\\=", "\\:", "\\#", BS + "u0041", BS + "u00e9", "x", "1", ".", "\\\n", "\\\r\n",
            String.valueOf((char) 0xE9),
        };
        Random random = new Random(7L);
        List<String> failures = new ArrayList<String>();
        for (int i = 0; i < 20000; i++) {
            StringBuilder text = new StringBuilder();
            int n = random.nextInt(14);
            for (int j = 0; j < n; j++) {
                text.append(pieces[random.nextInt(pieces.length)]);
            }
            if (commentEndsInBackslash(text.toString())) {
                continue;
            }
            String expected = outcome(text.toString(), true);
            String actual = outcome(text.toString(), false);
            if (!expected.equals(actual)) {
                failures.add(show(text.toString()) + ": jdk=" + show(expected) + " shim=" + show(actual));
            }
        }
        report(failures);
    }

    /// Whether a line that may be a comment ends in a backslash.
    ///
    /// Intended difference. The JDK's line reader counts backslashes through
    /// a comment and does not start again at the next line, so a comment
    /// ending in one changes how the following line is read -- to the point
    /// of reading characters left in its buffer by earlier lines. A comment
    /// cannot be continued in this format, and the shim does not let it
    /// reach past its own line.
    private static boolean commentEndsInBackslash(String text) {
        int lineStart = 0;
        for (int i = 0; i <= text.length(); i++) {
            if (i < text.length() && text.charAt(i) != '\n' && text.charAt(i) != '\r') {
                continue;
            }
            int first = lineStart;
            while (first < i && (text.charAt(first) == ' ' || text.charAt(first) == '\t'
                    || text.charAt(first) == '\f')) {
                first++;
            }
            if (first < i && (text.charAt(first) == '#' || text.charAt(first) == '!')
                    && text.charAt(i - 1) == '\\') {
                return true;
            }
            lineStart = i + 1;
        }
        return false;
    }

    @Test
    public void commentsDoNotReachTheNextLine() {
        // Where the JDK and the shim part ways, pinned down.
        assertEquals("{b=1}", shimParse("# comment \\\nb=1").toString());
        assertEquals("{b=1}", shimParse("! comment \\\r\n  b=1").toString());
    }

    @Test
    public void readsStreamsAsLatin1LikeTheJdk() throws IOException {
        byte[] bytes = {'k', '=', (byte) 0xE9, (byte) 0xFF, '\n', 'u', '=', '\\', 'u', '4', 'E', '2', 'D', '\n'};
        Properties properties = new Properties();
        properties.load(new ByteArrayInputStream(bytes));
        PropertyResourceBundle bundle = new PropertyResourceBundle(new ByteArrayInputStream(bytes));
        assertEquals(properties.getProperty("k"), bundle.getString("k"));
        assertEquals(properties.getProperty("u"), bundle.getString("u"));
        assertEquals(new TreeSet<String>(properties.stringPropertyNames()), new TreeSet<String>(bundle.keySet()));

        PropertyResourceBundle fromReader = new PropertyResourceBundle(new StringReader("a=1\nb=2"));
        assertEquals("1", fromReader.getString("a"));
        assertEquals("2", fromReader.getObject("b"));
        assertTrue(fromReader.containsKey("a"));
        assertFalse(fromReader.containsKey("c"));
    }

    // ------------------------------------------------------------------
    // Lookup
    // ------------------------------------------------------------------

    private static String describe(java.util.ResourceBundle bundle) {
        StringBuilder sb = new StringBuilder();
        for (String key : new TreeSet<String>(bundle.keySet())) {
            sb.append(key).append('=').append(bundle.getString(key)).append(';');
        }
        return sb.append(" locale=").append(bundle.getLocale()).toString();
    }

    private static String describe(ResourceBundle bundle) {
        StringBuilder sb = new StringBuilder();
        for (String key : new TreeSet<String>(bundle.keySet())) {
            sb.append(key).append('=').append(bundle.getString(key)).append(';');
        }
        Locale locale = bundle.getLocale();
        String name = locale.getLanguage();
        if (locale.getCountry().length() > 0) {
            name = name + "_" + locale.getCountry();
        }
        return sb.append(" locale=").append(name).toString();
    }

    @Test
    public void resolvesLocalesLikeTheJdk() {
        Locale[] locales = {
            new Locale("", ""), new Locale("fr", ""), new Locale("fr", "CA"), new Locale("fr", "FR"),
            new Locale("de", ""), new Locale("de", "DE"), new Locale("en", "US"), new Locale("en", "GB"),
            new Locale("es", "MX"),
        };
        Locale[] defaults = {Locale.US, new Locale("fr", "CA"), new Locale("fr", "BE"), new Locale("de", "DE")};
        for (Locale def : defaults) {
            Locale.setDefault(def);
            for (Locale locale : locales) {
                ResourceBundle.clearCache();
                java.util.ResourceBundle.clearCache();
                assertEquals("default " + def + ", asked " + locale,
                        describe(java.util.ResourceBundle.getBundle(BASE, locale)),
                        describe(ResourceBundle.getBundle(BASE, locale)));
            }
            ResourceBundle.clearCache();
            java.util.ResourceBundle.clearCache();
            assertEquals(describe(java.util.ResourceBundle.getBundle(BASE)), describe(ResourceBundle.getBundle(BASE)));
        }
    }

    @Test
    public void chainsToLessSpecificBundles() {
        ResourceBundle bundle = ResourceBundle.getBundle(BASE, new Locale("fr", "CA"));
        assertEquals("Allo", bundle.getString("greeting"));
        assertEquals("Au revoir", bundle.getString("farewell"));
        assertEquals("fr", bundle.getString("only.fr"));
        assertEquals("base", bundle.getString("only.base"));
        assertEquals("base", bundle.getObject("only.base"));
        assertTrue(bundle.containsKey("only.base"));
        assertFalse(bundle.containsKey("absent"));
        assertEquals(new TreeSet<String>(java.util.ResourceBundle.getBundle(BASE, new Locale("fr", "CA")).keySet()),
                new TreeSet<String>(bundle.keySet()));
        List<String> keys = new ArrayList<String>();
        for (Enumeration<String> e = bundle.getKeys(); e.hasMoreElements();) {
            keys.add(e.nextElement());
        }
        Collections.sort(keys);
        assertEquals("[farewell, greeting, only.base, only.fr]", keys.toString());
        // Slashes name the same bundle as dots.
        assertSame(bundle, ResourceBundle.getBundle(BASE.replace('.', '/'), new Locale("fr", "CA")));
        // And the lookup is cached until told otherwise.
        assertSame(bundle, ResourceBundle.getBundle(BASE, new Locale("fr", "CA"), null));
        ResourceBundle.clearCache();
        assertNotSame(bundle, ResourceBundle.getBundle(BASE, new Locale("fr", "CA")));
    }

    @Test
    public void reportsWhatIsMissingLikeTheJdk() {
        ResourceBundle bundle = ResourceBundle.getBundle(BASE, new Locale("fr", ""));
        try {
            bundle.getString("absent");
            fail("absent key");
        } catch (MissingResourceException e) {
            try {
                java.util.ResourceBundle.getBundle(BASE, new Locale("fr", "")).getString("absent");
                fail("absent key in the JDK");
            } catch (java.util.MissingResourceException theirs) {
                assertEquals(theirs.getKey(), e.getKey());
                assertEquals(theirs.getMessage().replace("java.util.PropertyResourceBundle",
                        PropertyResourceBundle.class.getName()), e.getMessage());
            }
        }
        try {
            ResourceBundle.getBundle("com.codename1.compat.jdk.bundles.Nothing", new Locale("fr", "CA"));
            fail("absent bundle");
        } catch (MissingResourceException e) {
            try {
                java.util.ResourceBundle.getBundle("com.codename1.compat.jdk.bundles.Nothing", new Locale("fr", "CA"));
                fail("absent bundle in the JDK");
            } catch (java.util.MissingResourceException theirs) {
                assertEquals(theirs.getMessage(), e.getMessage());
                assertEquals(theirs.getClassName(), e.getClassName());
                assertEquals(theirs.getKey(), e.getKey());
            }
        }
        try {
            ResourceBundle.getBundle(null);
            fail("null name");
        } catch (NullPointerException expected) {
            // As the JDK.
        }
        // A bundle with no base file exists only for its own language.
        assertEquals("nur deutsch", ResourceBundle
                .getBundle("com.codename1.compat.jdk.bundles.NoBase", new Locale("de", "AT")).getString("key"));
        try {
            ResourceBundle.getBundle("com.codename1.compat.jdk.bundles.NoBase", new Locale("fr", ""));
            fail("no French and no base");
        } catch (MissingResourceException expected) {
            // As the JDK.
        }
    }

    @Test
    public void registeredResourcesComeBeforeThePathLookup() {
        String dir = "/com/codename1/compat/jdk/registered/";
        // A name with no file at the conventional path at all.
        ResourceBundle.cn1Register("app.Texts", "", dir + "odd-name.txt");
        ResourceBundle.cn1Register("app/Texts", "_es", "com/codename1/compat/jdk/registered/odd-name-es.txt");
        assertEquals("Registered", ResourceBundle.getBundle("app.Texts", new Locale("it", "")).getString("greeting"));
        assertEquals("Registrado", ResourceBundle.getBundle("app.Texts", new Locale("es", "MX")).getString("greeting"));
        assertEquals("es", ResourceBundle.getBundle("app.Texts", new Locale("es", "MX")).getLocale().getLanguage());

        // A registration wins over the file the path lookup would find, and
        // takes effect although the bundle was already loaded.
        assertEquals("Bonjour", ResourceBundle.getBundle(BASE, new Locale("fr", "")).getString("greeting"));
        ResourceBundle.cn1Register(BASE, "_fr", dir + "odd-name.txt");
        try {
            ResourceBundle french = ResourceBundle.getBundle(BASE, new Locale("fr", ""));
            assertEquals("Registered", french.getString("greeting"));
            // The parent chain is unaffected.
            assertEquals("base", french.getString("only.base"));
        } finally {
            // Registrations outlive clearCache(); put the real file back.
            ResourceBundle.cn1Register(BASE, "_fr", "/com/codename1/compat/jdk/bundles/Messages_fr.properties");
        }
        assertEquals("Bonjour", ResourceBundle.getBundle(BASE, new Locale("fr", "")).getString("greeting"));
        try {
            ResourceBundle.cn1Register(null, "", "x");
            fail("null base name");
        } catch (NullPointerException expected) {
            // Nothing to register under.
        }
    }

    /// A bundle that is a class, found by name like a properties file.
    public static class ListBundle extends ListResourceBundle {
        @Override
        protected Object[][] getContents() {
            return new Object[][] {
                {"text", "from a class"},
                {"array", new String[] {"one", "two"}},
                {"number", Integer.valueOf(42)},
            };
        }
    }

    /// The French one; its parent is the class above.
    public static class ListBundle_fr extends ListResourceBundle {
        @Override
        protected Object[][] getContents() {
            return new Object[][] {{"text", "d'une classe"}};
        }
    }

    @Test
    public void loadsBundleClassesByName() {
        String name = ListBundle.class.getName();
        ResourceBundle base = ResourceBundle.getBundle(name, new Locale("it", ""));
        assertTrue(base instanceof ListBundle);
        assertEquals("from a class", base.getString("text"));
        assertArrayEquals(new String[] {"one", "two"}, base.getStringArray("array"));
        assertEquals(Integer.valueOf(42), base.getObject("number"));
        try {
            base.getString("number");
            fail("not a string");
        } catch (ClassCastException expected) {
            // As the JDK. Thrown explicitly by the shim, after an instanceof
            // test: a failed cast does not throw on every device.
        }
        ResourceBundle french = ResourceBundle.getBundle(name, new Locale("fr", "FR"));
        assertTrue(french instanceof ListBundle_fr);
        assertEquals("d'une classe", french.getString("text"));
        assertEquals(Integer.valueOf(42), french.getObject("number"));
        assertEquals("[array, number, text]", new TreeSet<String>(french.keySet()).toString());
    }

    private static String show(String text) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                sb.append("<LF>");
            } else if (c == '\r') {
                sb.append("<CR>");
            } else if (c == '\t') {
                sb.append("<TAB>");
            } else if (c == '\f') {
                sb.append("<FF>");
            } else if (c > 126) {
                sb.append("<").append(Integer.toHexString(c)).append(">");
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
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
