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
package com.codename1.tools.translator;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IteratorCensusTest {
    @Test
    void indexesEachAllocatingMethodOnceAndRejectsAnyLeakingSite() throws Exception {
        Parser.cleanup();
        Path dir = Files.createTempDirectory("iterator-census");
        Path source = dir.resolve("Census.java");
        String iterator = " implements java.util.Iterator<Object> {"
                + "public boolean hasNext(){return false;} public Object next(){return null;}"
                + "public void remove(){} }";
        Files.write(source, ("class Census {"
                + "static class Safe" + iterator + "static class Leaks" + iterator
                + "static Object escaped;"
                + "static Safe twice(){new Safe(); return new Safe();}"
                + "static Leaks returned(){return new Leaks();}"
                + "static void leaked(){escaped=new Leaks();}"
                + "}").getBytes(StandardCharsets.UTF_8));
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-source", "8", "-target", "8", "-d", dir.toString(), source.toString()));
        for (String name : new String[]{"Census", "Census$Safe", "Census$Leaks"})
            Parser.parse(dir.resolve(name + ".class").toFile());
        PrintStream previous = System.out;
        String property = System.getProperty("cn1.iteratorCensus");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setProperty("cn1.iteratorCensus", "true");
            System.setOut(new PrintStream(output, true, "UTF-8"));
            Parser.iteratorStackCensus();
            assertTrue(Parser.isStackIterator("Census_Safe"));
            assertFalse(Parser.isStackIterator("Census_Leaks"));
            String census = output.toString("UTF-8");
            assertTrue(census.contains("allocation sites=3 safe=2 escaping=1 unanalysable=0"), census);
        } finally {
            System.setOut(previous);
            if (property == null) System.clearProperty("cn1.iteratorCensus");
            else System.setProperty("cn1.iteratorCensus", property);
            Parser.cleanup();
        }
    }
}
