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
