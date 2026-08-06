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
package com.codename1.hardening;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Command-line front end. The engine runs as a forked process so it is single-sourced
 * across the maven plugin and the cloud daemon and cannot drift between them, and so
 * its ProGuard/ASM never share a classloader with either caller.
 *
 * <pre>
 *   java -jar cn1-hardening.jar harden --in in.jar --out out.jar \
 *        --mapping mapping.txt --report report.json --config config.properties
 * </pre>
 *
 * <p>The {@code config.properties} carries the resolved {@code harden.*} hints plus
 * {@code cn1.platform}, {@code cn1.mainClass}, {@code cn1.renameSupported},
 * {@code cn1.entitled}, {@code cn1.buildKey} and {@code cn1.libraryJars}. Exit codes:
 * {@code 0} hardened, {@code 3} declined by config (caller keeps the input jar),
 * {@code 4} not entitled, anything else a failure.
 */
public final class Main {

    public static final int EXIT_HARDENED = 0;
    public static final int EXIT_FAILED = 1;
    public static final int EXIT_DECLINED = 3;
    public static final int EXIT_NOT_ENTITLED = 4;

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        try {
            if (args.length == 0 || !"harden".equals(args[0])) {
                System.err.println("usage: harden --in <jar> --out <jar> --mapping <file> "
                        + "--report <file> --config <properties>");
                return EXIT_FAILED;
            }
            Map<String, String> opts = parseOptions(args);
            File in = fileOpt(opts, "in");
            File out = fileOpt(opts, "out");
            File mapping = fileOpt(opts, "mapping");
            File report = opts.containsKey("report") ? new File(opts.get("report")) : null;
            File configFile = fileOpt(opts, "config");

            Properties props = new Properties();
            FileInputStream fi = new FileInputStream(configFile);
            try {
                props.load(fi);
            } finally {
                fi.close();
            }

            String platform = props.getProperty("cn1.platform", "unknown");
            String mainClass = props.getProperty("cn1.mainClass", "");
            boolean renameSupported = !"false".equalsIgnoreCase(props.getProperty("cn1.renameSupported", "true"));
            boolean entitled = !"false".equalsIgnoreCase(props.getProperty("cn1.entitled", "true"));
            String buildKey = props.getProperty("cn1.buildKey", "");

            Map<String, String> hints = new HashMap<String, String>();
            for (String name : props.stringPropertyNames()) {
                if (name.startsWith("harden.")) {
                    hints.put(name, props.getProperty(name));
                }
            }

            HardeningConfig cfg = HardeningConfig.from(hints, platform, renameSupported);

            if (cfg.getProfile() != HardeningProfile.OFF && !entitled) {
                System.err.println("App hardening is an Enterprise feature and this build is not "
                        + "entitled. Refusing to produce a half-hardened binary.");
                return EXIT_NOT_ENTITLED;
            }

            HardeningRequest req = new HardeningRequest()
                    .inputJar(in)
                    .outputJar(out)
                    .mappingFile(mapping)
                    .reportFile(report)
                    .workDir(out.getAbsoluteFile().getParentFile())
                    .config(cfg)
                    .mainClass(mainClass)
                    .buildKey(buildKey);
            for (File lib : libraryJars(props)) {
                req.addLibraryJar(lib);
            }

            HardeningResult result = HardeningEngine.harden(req);
            if (!result.isHardened()) {
                System.out.println("cn1-hardening: skipped (" + result.getOutcome() + ")");
                return EXIT_DECLINED;
            }
            System.out.println("cn1-hardening: hardened " + result.getClassesOut() + " classes, "
                    + "renamed " + result.getRenamedClasses() + ", encrypted "
                    + result.getEncryptedStrings() + " strings, transforms="
                    + result.getTransformsApplied() + ", mappingId=" + result.getMappingId());
            for (String w : result.getWarnings()) {
                System.out.println("cn1-hardening: warning: " + w);
            }
            return EXIT_HARDENED;
        } catch (HardeningException e) {
            System.err.println("cn1-hardening: " + e.getMessage());
            return EXIT_FAILED;
        } catch (Exception e) {
            System.err.println("cn1-hardening: unexpected failure: " + e);
            e.printStackTrace();
            return EXIT_FAILED;
        }
    }

    private static java.util.List<File> libraryJars(Properties props) {
        java.util.List<File> jars = new java.util.ArrayList<File>();
        String raw = props.getProperty("cn1.libraryJars", "");
        if (raw != null && !raw.isEmpty()) {
            for (String p : raw.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
                if (!p.trim().isEmpty()) {
                    jars.add(new File(p.trim()));
                }
            }
        }
        return jars;
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> opts = new HashMap<String, String>();
        for (int i = 1; i < args.length - 1; i++) {
            if (args[i].startsWith("--")) {
                opts.put(args[i].substring(2), args[i + 1]);
                i++;
            }
        }
        return opts;
    }

    private static File fileOpt(Map<String, String> opts, String key) throws HardeningException {
        String v = opts.get(key);
        if (v == null) {
            throw new HardeningException("missing --" + key);
        }
        return new File(v);
    }
}
