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
package com.codename1.maven;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/// The JDK stand-ins, end to end: source written against the JDK is compiled,
/// run, relocated by the remap step and run again, and has to answer the same.
public class JdkShimsRemapTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private RemapDifferential java8() throws Exception {
        return new RemapDifferential(tmp.newFolder(), 8);
    }

    @Test
    public void textIsEncodedAndDecodedByCharset() throws Exception {
        java8().same(""
                + "byte[] utf = \"caf\\u00e9 \\u20ac\".getBytes(StandardCharsets.UTF_8);\n"
                + "out.add(utf);\n"
                + "out.add(new String(utf, StandardCharsets.UTF_8));\n"
                + "out.add(new String(utf, 0, 3, StandardCharsets.US_ASCII));\n"
                + "out.add(new String(utf, 1, 4, StandardCharsets.ISO_8859_1).length());\n"
                + "out.add(StandardCharsets.UTF_8.name());\n"
                + "out.add(StandardCharsets.ISO_8859_1.toString());\n"
                + "out.add(Charset.forName(\"utf-8\").name());\n"
                + "out.add(Charset.forName(\"UTF8\") == StandardCharsets.UTF_8);\n"
                + "out.add(Charset.forName(\"latin1\").name());\n"
                + "out.add(Charset.forName(\"ascii\").name());\n"
                + "out.add(Charset.isSupported(\"UTF-8\"));\n"
                + "out.add(Charset.defaultCharset() != null);\n"
                + "Reader r = new InputStreamReader(new ByteArrayInputStream(utf), StandardCharsets.UTF_8);\n"
                + "StringBuilder sb = new StringBuilder();\n"
                + "for (int c = r.read(); c >= 0; c = r.read()) { sb.append((char) c); }\n"
                + "out.add(sb.toString());\n"
                + "ByteArrayOutputStream bytes = new ByteArrayOutputStream();\n"
                + "Writer w = new OutputStreamWriter(bytes, StandardCharsets.UTF_8);\n"
                + "w.write(\"na\\u00efve\");\n"
                + "w.close();\n"
                + "out.add(bytes.toByteArray());\n"
                + "out.add(bytes.toString(\"UTF-8\"));\n", 14);
    }

    @Test
    public void regularExpressionsAreTheJdks() throws Exception {
        RemapDifferential d = new RemapDifferential(tmp.newFolder(), 8);
        d.same(""
                + "Pattern p = Pattern.compile(\"(?<key>[a-z]+)=(\\\\d+)\", Pattern.CASE_INSENSITIVE);\n"
                + "Matcher m = p.matcher(\"Alpha=1, beta=22; GAMMA=333\");\n"
                + "while (m.find()) {\n"
                + "    out.add(m.group(\"key\") + \":\" + m.group(2) + \"@\" + m.start() + \"-\" + m.end());\n"
                + "}\n"
                + "out.add(m.replaceAll(\"$2=${key}\"));\n"
                + "out.add(p.split(\"x a=1 y b=2 z\"));\n"
                + "out.add(\"a1b22c\".split(\"\\\\d+\"));\n"
                + "out.add(\"a,b,,\".split(\",\"));\n"
                + "out.add(\"a,b,,\".split(\",\", -1));\n"
                + "out.add(\"2024-02-29\".matches(\"\\\\d{4}-\\\\d{2}-\\\\d{2}\"));\n"
                + "out.add(\"  trim  me \".replaceAll(\"^\\\\s+|\\\\s+$\", \"\"));\n"
                + "out.add(\"camelCaseName\".replaceAll(\"(?<=[a-z])(?=[A-Z])\", \"_\"));\n"
                + "out.add(\"a.b.c\".replaceFirst(\"\\\\.\", \"/\"));\n"
                + "out.add(Pattern.matches(\"[\\\\w.]+@[\\\\w.]+\", \"me@example.com\"));\n"
                + "out.add(Pattern.quote(\"a.b\"));\n"
                + "out.add(Matcher.quoteReplacement(\"$1\"));\n"
                + "try {\n"
                + "    Pattern.compile(\"(unclosed\");\n"
                + "} catch (PatternSyntaxException e) {\n"
                + "    out.add(e.getIndex() + \"|\" + e.getPattern() + \"|\" + e.getDescription());\n"
                + "}\n"
                + "MatchResult r = Pattern.compile(\"b+\").matcher(\"abbbc\");\n"
                + "out.add(((Matcher) r).find() ? r.group() + r.start() + r.end() + r.groupCount() : \"none\");\n"
                + "out.add(Pattern.compile(\"\\\\s*,\\\\s*\").splitAsStream(\"a , b,c\").collect(Collectors.toList()));\n"
                + "out.add(Pattern.compile(\"^\\\\d+$\").asPredicate().test(\"123\"));\n"
                + "StringBuffer sb = new StringBuffer();\n"
                + "Matcher d2 = Pattern.compile(\"\\\\d\").matcher(\"a1b2\");\n"
                + "while (d2.find()) {\n"
                + "    d2.appendReplacement(sb, \"<\" + d2.group() + \">\");\n"
                + "}\n"
                + "out.add(d2.appendTail(sb).toString());\n", 20);
    }
}
