/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a compiled backend test binary reported, read from its CN1TEST lines (see
 * {@code com.codename1.impl.backend.test.TestRun}) and written as Surefire XML.
 */
final class CompiledTestReport {
    int passed;
    int failed;
    int skipped;
    boolean done;
    /** class -> its cases, as {kind, method, millis, type, message}. */
    final Map<String, List<String[]>> cases = new LinkedHashMap<String, List<String[]>>();

    static CompiledTestReport parse(List<String> lines) {
        CompiledTestReport r = new CompiledTestReport();
        for (String line : lines) {
            if (!line.startsWith("CN1TEST\t")) {
                continue;
            }
            String[] f = line.split("\t", -1);
            if ("DONE".equals(f[1])) {
                r.done = true;
                continue;
            }
            if (f.length < 5) {
                continue;
            }
            String kind = f[1];
            String[] c = {kind, unescape(f[3]), f[4], f.length > 5 ? f[5] : null,
                    f.length > 6 ? unescape(f[6]) : null};
            List<String[]> list = r.cases.get(f[2]);
            if (list == null) {
                list = new ArrayList<String[]>();
                r.cases.put(f[2], list);
            }
            list.add(c);
            if ("PASS".equals(kind)) {
                r.passed++;
            } else if ("FAIL".equals(kind)) {
                r.failed++;
            } else if ("SKIP".equals(kind)) {
                r.skipped++;
            }
        }
        return r;
    }

    String failures() {
        return list("FAIL");
    }

    String skips() {
        return list("SKIP");
    }

    private String list(String kind) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String[]>> e : cases.entrySet()) {
            for (String[] c : e.getValue()) {
                if (kind.equals(c[0])) {
                    sb.append("  ").append(e.getKey()).append('.').append(c[1]).append(": ")
                      .append(c[4]).append('\n');
                }
            }
        }
        return sb.toString();
    }

    void write(File dir) throws IOException {
        Files.createDirectories(dir.toPath());
        for (Map.Entry<String, List<String[]>> e : cases.entrySet()) {
            int f = 0;
            int s = 0;
            long total = 0;
            for (String[] c : e.getValue()) {
                f += "FAIL".equals(c[0]) ? 1 : 0;
                s += "SKIP".equals(c[0]) ? 1 : 0;
                total += parseLong(c[2]);
            }
            StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            sb.append("<testsuite name=\"").append(xml(e.getKey())).append(" (compiled)\" tests=\"")
              .append(e.getValue().size()).append("\" failures=\"").append(f)
              .append("\" errors=\"0\" skipped=\"").append(s).append("\" time=\"")
              .append(seconds(total)).append("\">\n");
            for (String[] c : e.getValue()) {
                sb.append("  <testcase classname=\"").append(xml(e.getKey())).append("\" name=\"")
                  .append(xml(c[1])).append(" [compiled]\" time=\"").append(seconds(parseLong(c[2])))
                  .append('"');
                if ("FAIL".equals(c[0])) {
                    sb.append(">\n    <failure type=\"").append(xml(c[3])).append("\" message=\"")
                      .append(xml(c[4])).append("\">").append(xml(c[4])).append("</failure>\n")
                      .append("  </testcase>\n");
                } else if ("SKIP".equals(c[0])) {
                    sb.append(">\n    <skipped message=\"").append(xml(c[4])).append("\"/>\n")
                      .append("  </testcase>\n");
                } else {
                    sb.append("/>\n");
                }
            }
            sb.append("</testsuite>\n");
            Files.write(new File(dir, "TEST-" + e.getKey() + "-compiled.xml").toPath(),
                    sb.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (RuntimeException err) {
            return 0;
        }
    }

    private static String seconds(long millis) {
        return String.valueOf(millis / 1000.0);
    }

    static String unescape(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char n = value.charAt(++i);
                sb.append(n == 't' ? '\t' : n == 'n' ? '\n' : n == 'r' ? '\r' : n);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String xml(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '&': sb.append("&amp;"); break;
                case '"': sb.append("&quot;"); break;
                case '\n': sb.append("&#10;"); break;
                case '\t': sb.append("&#9;"); break;
                default:
                    if (c < 0x20 && c != '\r') {
                        sb.append(' ');
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
