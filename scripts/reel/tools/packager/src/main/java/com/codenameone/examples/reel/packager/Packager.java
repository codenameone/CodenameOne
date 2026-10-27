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
package com.codenameone.examples.reel.packager;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Command line entry point: `package`, `worker` and `seed`.
public final class Packager {
    private Packager() {
    }

    /// Parses `--name value` and `--flag` arguments.
    static Map<String, String> options(String[] args, int from) {
        Map<String, String> m = new HashMap<>();
        for (int i = from; i < args.length; i++) {
            if (!args[i].startsWith("--")) {
                throw new IllegalArgumentException("Unexpected argument " + args[i]);
            }
            String k = args[i].substring(2);
            if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                m.put(k, args[++i]);
            } else {
                m.put(k, "true");
            }
        }
        return m;
    }

    private static String need(Map<String, String> o, String k) {
        String v = o.get(k);
        if (v == null) {
            throw new IllegalArgumentException("Missing --" + k);
        }
        return v;
    }

    private static int[] ints(String csv) {
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).mapToInt(Integer::parseInt).toArray();
    }

    /// Runs the tool.
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            System.exit(2);
        }
        try {
            Map<String, String> o = options(args, 1);
            switch (args[0]) {
                case "package" -> {
                    PackageJob j = new PackageJob();
                    j.input = Path.of(need(o, "input"));
                    j.out = Path.of(need(o, "out"));
                    j.keyId = need(o, "key-id");
                    j.key = need(o, "key");
                    j.iv = need(o, "iv");
                    if (o.containsKey("renditions")) {
                        j.renditions = ints(o.get("renditions"));
                    }
                    if (o.containsKey("segment")) {
                        j.segment = Integer.parseInt(o.get("segment"));
                    }
                    j.progress = p -> System.out.println("progress " + p + "%");
                    j.run();
                    System.out.println("wrote " + j.out);
                }
                case "worker" -> new Worker(need(o, "server"), need(o, "token"),
                        o.getOrDefault("name", "worker-" + ProcessHandle.current().pid()))
                        .run(o.containsKey("once"));
                case "seed" -> {
                    List<String> films = Arrays.asList(o.getOrDefault("films", "bbb,sintel,tos,ed").split(","));
                    new Seed(Path.of(need(o, "media")), o.containsKey("small")).run(films);
                }
                default -> {
                    usage();
                    System.exit(2);
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            usage();
            System.exit(2);
        }
    }

    private static void usage() {
        System.err.println("usage:\n"
                + "  package --input <file> --out <dir> --key-id <hex32> --key <hex32> --iv <hex32>"
                + " [--renditions 720,480,360] [--segment 6]\n"
                + "  worker --server <url> --token <t> [--name <n>] [--once]\n"
                + "  seed --media <dir> [--films bbb,sintel,tos,ed] [--small]");
    }
}
