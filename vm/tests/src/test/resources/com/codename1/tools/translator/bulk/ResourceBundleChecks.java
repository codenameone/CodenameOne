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

import com.codename1.impl.JdkHost;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.TreeSet;

/// java.util.ResourceBundle and PropertyResourceBundle: the `.properties`
/// format, and the lookup of a bundle by name and locale over the files in
/// `res/com/codename1/bulk/bundles` and the bundle classes below.
public final class ResourceBundleChecks {
    private static final String BASE = "com.codename1.bulk.bundles.Messages";

    // A backslash, kept out of string literals next to a 'u': javac reads
    // that pair as an escape wherever it appears.
    private static final String BS = String.valueOf((char) 92);

    private ResourceBundleChecks() {
    }

    /// The name of [ResourceBundleList], spelled out: ParparVM's `getName`
    /// reports an '_' in a class name as a '.', so `ResourceBundleList_fr`
    /// cannot be named through its class.
    private static final String LIST = "ResourceBundleList";

    /// What the build generates for an application: its bundle classes, made
    /// without reflection. With `french` false only the base class is
    /// registered.
    private static final class ListRegistry implements JdkHost.Registry {
        private final boolean french;

        ListRegistry(boolean french) {
            this.french = french;
        }

        @Override
        public String[] providers(String service) {
            return null;
        }

        @Override
        public Object newInstance(String className) {
            if (LIST.equals(className)) {
                return new ResourceBundleList();
            }
            return french && (LIST + "_fr").equals(className) ? new ResourceBundleList_fr() : null;
        }

        @Override
        public Class<?> type(String className) {
            if (LIST.equals(className)) {
                return ResourceBundleList.class;
            }
            return french && (LIST + "_fr").equals(className) ? ResourceBundleList_fr.class : null;
        }
    }

    /// A check's body run with the default locale `Locale.US` and empty
    /// bundle caches, the default restored afterwards.
    private static Bulk.Body inUs(final Bulk.Body body) {
        return () -> {
            Locale saved = Locale.getDefault();
            Locale.setDefault(Locale.US);
            ResourceBundle.clearCache();
            try {
                body.run();
            } finally {
                Locale.setDefault(saved);
                ResourceBundle.clearCache();
            }
        };
    }

    /// Runs the group.
    public static void run() {
        Bulk.check("ResourceBundle.parsesPropertiesLikeTheJdk", () -> {
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
            for (int i = 0; i < texts.length; i++) {
                Bulk.p("t" + i + " " + show(texts[i]), outcome(texts[i]));
            }
        });
        Bulk.check("ResourceBundle.rejectsMalformedEscapesLikeTheJdk", () -> {
            String[] texts = {
                "a=" + BS + "u12", "a=" + BS + "uXYZW", "a=" + BS + "u", "a=" + BS + "u123", BS + "u00G0=1",
                "a=" + BS + "u12\nb=2",
            };
            for (int i = 0; i < texts.length; i++) {
                Bulk.p("t" + i + " " + show(texts[i]), outcome(texts[i]));
            }
        });
        // After Java 8 the JDK reads a comment marker that follows an empty
        // continued line as a comment. The texts on which the HotSpot run's
        // JDK and Java 8 agree are compared here; the others are held to Java
        // 8's answers, written out, by the check after this one.
        Bulk.check("ResourceBundle.parsesRandomPropertiesLikeTheJdk", () -> {
            List<String> texts = randomTexts();
            java.util.Set<Integer> java8Only = new java.util.HashSet<Integer>();
            for (int index : ResourceBundleJava8Table.INDEX) {
                java8Only.add(Integer.valueOf(index));
            }
            int hash = 0;
            int compared = 0;
            for (int i = 0; i < texts.size(); i++) {
                String text = texts.get(i);
                if (text == null || java8Only.contains(Integer.valueOf(i))) {
                    continue;
                }
                hash = 31 * hash + outcome(text).hashCode();
                compared++;
                if (i % 2000 == 1999) {
                    Bulk.p("upTo" + i, hash + " over " + compared);
                }
            }
            Bulk.p("all", hash + " over " + compared);
        });
        Bulk.cn1Only("ResourceBundle.parsesRandomPropertiesLikeTheJdkOnJava8", () -> {
            List<String> texts = randomTexts();
            for (int k = 0; k < ResourceBundleJava8Table.INDEX.length; k++) {
                int i = ResourceBundleJava8Table.INDEX[k];
                Bulk.expect(texts.get(i) != null, "text " + i + " is not one of the skipped ones");
                if (texts.get(i) != null) {
                    Bulk.expectEquals(ResourceBundleJava8Table.OUTCOME[k], outcome(texts.get(i)),
                            "text " + i + " " + show(texts.get(i)));
                }
            }
        });
        Bulk.cn1Only("ResourceBundle.commentsDoNotReachTheNextLine", () -> {
            // Where the JDK and JavaAPI part ways, pinned down.
            Bulk.expectEquals("{b=1}", parse("# comment \\\nb=1").toString(), "hash comment");
            Bulk.expectEquals("{b=1}", parse("! comment \\\r\n  b=1").toString(), "bang comment");
        });
        Bulk.check("ResourceBundle.readsStreamsAsLatin1LikeTheJdk", () -> {
            byte[] bytes = {'k', '=', (byte) 0xE9, (byte) 0xFF, '\n', 'u', '=', '\\', 'u', '4', 'E', '2', 'D', '\n'};
            PropertyResourceBundle bundle = new PropertyResourceBundle(new ByteArrayInputStream(bytes));
            Bulk.p("k", bundle.getString("k"));
            Bulk.p("u", bundle.getString("u"));
            Bulk.p("keys", new TreeSet<String>(bundle.keySet()));
            PropertyResourceBundle fromReader = new PropertyResourceBundle(new StringReader("a=1\nb=2"));
            Bulk.p("a", fromReader.getString("a"));
            Bulk.p("b", fromReader.getObject("b"));
            Bulk.p("containsA", fromReader.containsKey("a"));
            Bulk.p("containsC", fromReader.containsKey("c"));
        });
        Bulk.check("ResourceBundle.resolvesLocalesLikeTheJdk", inUs(() -> {
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
                    Bulk.p("default " + def + " asked " + locale, describe(ResourceBundle.getBundle(BASE, locale)));
                }
                ResourceBundle.clearCache();
                Bulk.p("default " + def, describe(ResourceBundle.getBundle(BASE)));
            }
        }));
        Bulk.check("ResourceBundle.chainsToLessSpecificBundles", inUs(() -> {
            ResourceBundle bundle = ResourceBundle.getBundle(BASE, new Locale("fr", "CA"));
            Bulk.p("greeting", bundle.getString("greeting"));
            Bulk.p("farewell", bundle.getString("farewell"));
            Bulk.p("only.fr", bundle.getString("only.fr"));
            Bulk.p("only.base", bundle.getString("only.base"));
            Bulk.p("only.base object", bundle.getObject("only.base"));
            Bulk.p("containsBase", bundle.containsKey("only.base"));
            Bulk.p("containsAbsent", bundle.containsKey("absent"));
            Bulk.p("keySet", new TreeSet<String>(bundle.keySet()));
            List<String> keys = new ArrayList<String>();
            for (Enumeration<String> e = bundle.getKeys(); e.hasMoreElements();) {
                keys.add(e.nextElement());
            }
            Collections.sort(keys);
            Bulk.p("getKeys", keys);
            // The lookup is cached until told otherwise.
            Bulk.p("cached", bundle == ResourceBundle.getBundle(BASE, new Locale("fr", "CA")));
            ResourceBundle.clearCache();
            Bulk.p("afterClear", bundle == ResourceBundle.getBundle(BASE, new Locale("fr", "CA")));
        }));
        // The JDK caches by the name as given, so the slashed name is another
        // entry there, and it refuses a null class loader.
        Bulk.cn1Only("ResourceBundle.chainsToLessSpecificBundlesCn1", inUs(() -> {
            ResourceBundle bundle = ResourceBundle.getBundle(BASE, new Locale("fr", "CA"));
            Bulk.expect(bundle == ResourceBundle.getBundle(BASE.replace('.', '/'), new Locale("fr", "CA")),
                    "slashes name the same bundle as dots");
            Bulk.expect(bundle == ResourceBundle.getBundle(BASE, new Locale("fr", "CA"), (ClassLoader) null),
                    "a null loader answers the cached bundle");
        }));
        Bulk.check("ResourceBundle.reportsWhatIsMissingLikeTheJdk", inUs(() -> {
            ResourceBundle bundle = ResourceBundle.getBundle(BASE, new Locale("fr", ""));
            try {
                bundle.getString("absent");
                Bulk.p("absentKey", "none");
            } catch (MissingResourceException e) {
                Bulk.p("absentKey.key", e.getKey());
                Bulk.p("absentKey.message", e.getMessage());
            }
            try {
                ResourceBundle.getBundle("com.codename1.bulk.bundles.Nothing", new Locale("fr", "CA"));
                Bulk.p("absentBundle", "none");
            } catch (MissingResourceException e) {
                Bulk.p("absentBundle.message", e.getMessage());
                Bulk.p("absentBundle.className", e.getClassName());
                Bulk.p("absentBundle.key", e.getKey());
            }
            Bulk.pThrown("nullName", NullPointerException.class, () -> ResourceBundle.getBundle(null));
            // A bundle with no base file exists only for its own language.
            Bulk.p("noBaseDeAt", ResourceBundle
                    .getBundle("com.codename1.bulk.bundles.NoBase", new Locale("de", "AT")).getString("key"));
            Bulk.pThrown("noBaseFr", MissingResourceException.class,
                    () -> ResourceBundle.getBundle("com.codename1.bulk.bundles.NoBase", new Locale("fr", "")));
        }));
        // The JDK finds the classes by name, with reflection; ParparVM has none,
        // and a built application has the registry its build generates, so
        // that run is given one that knows both classes.
        Bulk.check("ResourceBundle.loadsBundleClassesByName", inUs(() -> {
            if (!Bulk.onHotSpot()) {
                JdkHost.setRegistry(new ListRegistry(true));
            }
            String name = LIST;
            ResourceBundle base = ResourceBundle.getBundle(name, new Locale("it", ""));
            Bulk.p("baseIsList", base instanceof ResourceBundleList);
            Bulk.p("text", base.getString("text"));
            Bulk.p("array", base.getStringArray("array"));
            Bulk.p("number", base.getObject("number"));
            Bulk.pThrown("numberAsString", ClassCastException.class, () -> base.getString("number"));
            ResourceBundle french = ResourceBundle.getBundle(name, new Locale("fr", "FR"));
            Bulk.p("frenchIsFrench", french instanceof ResourceBundleList_fr);
            Bulk.p("frenchText", french.getString("text"));
            Bulk.p("frenchNumber", french.getObject("number"));
            Bulk.p("frenchKeys", new TreeSet<String>(french.keySet()));
        }));
        Bulk.cn1Only("ResourceBundle.aBuiltApplicationHasTheBundleClassesItsRegistryKnowsAndItsFiles", inUs(() -> {
            JdkHost.setRegistry(new ListRegistry(false));
            ResourceBundle.clearCache();
            // The French class is not registered, so it is not looked up by
            // name: the chain ends at the class the registry knows.
            ResourceBundle french = ResourceBundle.getBundle(LIST, new Locale("fr", "FR"));
            Bulk.expect(french instanceof ResourceBundleList && !(french instanceof ResourceBundleList_fr),
                    "the registered class");
            Bulk.expectEquals("from a class", french.getString("text"), "text");
            // The properties files are still read.
            Bulk.expectEquals("Allo", ResourceBundle.getBundle(BASE, new Locale("fr", "CA")).getString("greeting"),
                    "greeting");
            Bulk.expectEquals("base", ResourceBundle.getBundle(BASE, new Locale("fr", "CA")).getString("only.base"),
                    "only.base");
        }));
    }

    /// The texts of the random comparison, generated as the original test
    /// generated them; null in place of one whose comment line ends in a
    /// backslash (see [#commentEndsInBackslash(String)]).
    private static List<String> randomTexts() {
        String[] pieces = {
            "a", "b", "key", "value", " ", "  ", "\t", "\f", "=", ":", "#", "!", "\\", "\\\\", "\n", "\r", "\r\n",
            "\\n", "\\t", "\\ ", "\\=", "\\:", "\\#", BS + "u0041", BS + "u00e9", "x", "1", ".", "\\\n", "\\\r\n",
            String.valueOf((char) 0xE9),
        };
        Random random = new Random(7L);
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < 20000; i++) {
            StringBuilder text = new StringBuilder();
            int n = random.nextInt(14);
            for (int j = 0; j < n; j++) {
                text.append(pieces[random.nextInt(pieces.length)]);
            }
            out.add(commentEndsInBackslash(text.toString()) ? null : text.toString());
        }
        return out;
    }

    /// Whether a line that may be a comment ends in a backslash.
    ///
    /// Intended difference. The JDK's line reader counts backslashes through
    /// a comment and does not start again at the next line, so a comment
    /// ending in one changes how the following line is read -- to the point
    /// of reading characters left in its buffer by earlier lines. A comment
    /// cannot be continued in this format, and JavaAPI does not let it reach
    /// past its own line.
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

    private static TreeMap<String, String> parse(String text) throws IOException {
        PropertyResourceBundle bundle = new PropertyResourceBundle(new StringReader(text));
        TreeMap<String, String> out = new TreeMap<String, String>();
        for (String key : bundle.keySet()) {
            out.put(key, bundle.getString(key));
        }
        return out;
    }

    private static String outcome(String text) {
        try {
            return parse(text).toString();
        } catch (IllegalArgumentException e) {
            return "IllegalArgumentException";
        } catch (IOException e) {
            return "IOException";
        }
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
}
