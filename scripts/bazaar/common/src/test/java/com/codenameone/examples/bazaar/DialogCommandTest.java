/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/// `Command.create(name, icon, null)` wraps a null listener and calls it when
/// the command fires, so a dialog button made that way throws a
/// NullPointerException the moment it is tapped (the inventory History
/// button did). A dialog button with no action is `new Command(name)`.
class DialogCommandTest {
    @Test
    void noCommandWrapsANullListener() throws IOException {
        List<String> found = new ArrayList<>();
        List<Path> sources;
        try (Stream<Path> files = Files.walk(Paths.get("src/main/java"))) {
            sources = files.filter(f -> f.toString().endsWith(".java")).collect(Collectors.toList());
        }
        assertTrue(sources.size() > 10, "sources not found");
        for (Path p : sources) {
            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String l = lines.get(i);
                boolean wrapped = l.contains("Command.create(") && l.trim().endsWith(", null,")
                        && i + 1 < lines.size() && lines.get(i + 1).trim().startsWith("null)");
                if (wrapped || l.contains("Command.create(") && l.contains(", null, null)")) {
                    found.add(p.getFileName() + ":" + (i + 1));
                }
            }
        }
        assertTrue(found.isEmpty(), "Command.create with a null listener: " + found);
    }
}
