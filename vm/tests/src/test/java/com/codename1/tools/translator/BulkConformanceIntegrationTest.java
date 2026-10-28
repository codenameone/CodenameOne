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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// The bulk conformance run: hundreds of named checks of the class library,
/// compiled once, run once on HotSpot (the JDK's `java.*`), translated once,
/// built once and run once on ParparVM (JavaAPI's), then compared check by
/// check. One translation and one CMake build replace the one each small
/// fixture used to pay for.
///
/// The program lives in `src/test/resources/com/codename1/tools/translator/bulk`:
///
/// - `Bulk.java` is the harness (see its comment for the rules a check follows)
///   and `BulkHost.java` the host the ParparVM run installs;
/// - every `*Checks.java` is a group with a `public static void run()`; the
///   main class calling them all is generated here, in file-name order;
/// - `res/` holds resources: on the HotSpot class path as files, and on
///   ParparVM served by `BulkHost` from a generated `BulkResources` class;
/// - a `*.c` file there is the native half of a check and is handed to the
///   translator beside the classes.
///
/// A failure names EVERY check that differs, not the first. To iterate on a few
/// groups, `-Dbulk.groups=OptionalChecks,LangChecks` builds a main class that
/// calls only those.
///
/// Further assertions on the generated C, which a folded fixture used to make
/// on its own translation, run here on the shared one.
class BulkConformanceIntegrationTest {

    private static final String BULK = "/com/codename1/tools/translator/bulk";

    @Test
    void everyCheckAnswersTheSameOnHotSpotAndParparVM() throws Exception {
        Parser.cleanup();
        Path bulkDir = bulkDir();
        Path work = Files.createTempDirectory("bulk-conformance");
        Path sourceDir = Files.createDirectories(work.resolve("sources"));
        Path classesDir = Files.createDirectories(work.resolve("classes"));
        Path javaApiDir = Files.createDirectories(work.resolve("javaapi"));

        List<String> groups = groups(bulkDir);
        assertTrue(!groups.isEmpty(), "no *Checks.java group under " + bulkDir);
        boolean subset = System.getProperty("bulk.groups", "").trim().length() > 0;
        List<Path> sources = new ArrayList<>();
        try (Stream<Path> files = Files.list(bulkDir)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).sorted().collect(Collectors.toList())) {
                if (subset && !belongsTo(p.getFileName().toString(), groups)) {
                    continue;
                }
                Path copy = sourceDir.resolve(p.getFileName().toString());
                Files.copy(p, copy);
                sources.add(copy);
            }
        }
        for (String shared : CORE_SOURCES) {
            sources.add(coreSources().resolve(shared));
        }
        Path main = sourceDir.resolve("BulkApp.java");
        Files.write(main, mainSource(groups).getBytes(StandardCharsets.UTF_8));
        sources.add(main);
        Path resourcesClass = sourceDir.resolve("BulkResources.java");
        Files.write(resourcesClass, resourcesSource(bulkDir.resolve("res")).getBytes(StandardCharsets.UTF_8));
        sources.add(resourcesClass);

        CompilerHelper.CompilerConfig config = selectCompiler();
        assertNotNull(config, "No JDK 17+ compiler that can build JavaAPI");
        CompilerHelper.compileJavaAPI(javaApiDir, config);
        List<String> compileArgs = new ArrayList<>(Arrays.asList("-source", config.targetVersion,
                "-target", config.targetVersion, "-encoding", "UTF-8", "-nowarn"));
        if (CompilerHelper.useClasspath(config)) {
            compileArgs.add("-classpath");
        } else {
            compileArgs.add("-Xlint:-options");
            compileArgs.add("-bootclasspath");
        }
        compileArgs.add(javaApiDir.toString());
        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        for (Path s : sources) {
            compileArgs.add(s.toString());
        }
        assertEquals(0, CompilerHelper.compile(config.jdkHome, compileArgs),
                "the bulk program should compile. " + CompilerHelper.getLastErrorLog());
        BulkGeneratedCodeChecks.patchClasses(classesDir);
        if (Files.isDirectory(bulkDir.resolve("res"))) {
            CompilerHelper.copyDirectory(bulkDir.resolve("res"), classesDir);
        }

        // HotSpot: the reference. JDK 19+ when there is one, for the reason
        // TaggedValueIntegrationTest gives: Double.toString changed in 19 and
        // ParparVM implements the new form.
        Path hotspotHome = referenceJdk(config);
        Path hotspotDir = Files.createDirectories(work.resolve("run-hotspot"));
        String javaExe = hotspotHome.resolve("bin").resolve(CompilerHelper.executableName("java")).toString();
        Run hotspot = run(Arrays.asList(javaExe, "-Dbulk.host=hotspot", "-Duser.language=en",
                "-Duser.country=US", "-cp", classesDir + java.io.File.pathSeparator + javaApiDir, "BulkApp"),
                hotspotDir);
        assertEquals(0, hotspot.exit, "the HotSpot run should exit cleanly:\n" + tail(hotspot.output));
        if (Boolean.getBoolean("bulk.hotspotOnly")) {
            // For writing checks: what the JDK answers, without the translation.
            System.out.println(hotspot.output);
            return;
        }

        // ParparVM.
        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        try (Stream<Path> files = Files.list(bulkDir)) {
            for (Path c : files.filter(f -> f.toString().endsWith(".c")).collect(Collectors.toList())) {
                Files.copy(c, classesDir.resolve(c.getFileName().toString()));
            }
        }
        Path outputDir = Files.createDirectories(work.resolve("output"));
        // CryptoChecks compares the native crypto kernels with the Java engine, so
        // this translation links them, as a builder does for an application using them.
        String originalKernels = System.getProperty("cn1.cryptoKernels");
        System.setProperty("cn1.cryptoKernels", "true");
        try {
            CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "BulkApp");
        } finally {
            if (originalKernels == null) {
                System.clearProperty("cn1.cryptoKernels");
            } else {
                System.setProperty("cn1.cryptoKernels", originalKernels);
            }
        }
        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");
        List<String> failures = new ArrayList<>();
        Path generated = distDir.resolve("BulkApp-src");
        BulkGeneratedCodeChecks.check(generated, groups, failures);

        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(cmakeLists, "BulkApp-src");
        Path buildDir = Files.createDirectories(distDir.resolve("build"));
        List<String> configure = new ArrayList<>(Arrays.asList("cmake", "-S", distDir.toString(), "-B",
                buildDir.toString()));
        configure.addAll(CompilerHelper.cmakeToolchainArgs());
        // CN1_TEST_EXTRA_CFLAGS, e.g. AddressSanitizer for the allocation
        // lifetime evidence StringBuilderGrowthApp was written to collect.
        configure.addAll(CompilerHelper.extraCFlagArgs());
        CleanTargetIntegrationTest.runCommand(configure, distDir);
        // One build, so it may use every core: this is the cost the bulk run
        // pays once instead of once per fixture.
        ProcessBuilder build = new ProcessBuilder("cmake", "--build", buildDir.toString(), "--parallel",
                String.valueOf(Math.max(2, Runtime.getRuntime().availableProcessors())));
        build.directory(distDir.toFile());
        CleanTargetIntegrationTest.runCommand(build, 45, TimeUnit.MINUTES);
        Path executable = buildDir.resolve(CompilerHelper.executableName("BulkApp"));
        assertTrue(Files.exists(executable), "ParparVM build should produce " + executable);
        Path parparDir = Files.createDirectories(work.resolve("run-parparvm"));
        Run parpar = run(Collections.singletonList(executable.toString()), parparDir);

        Map<String, List<String>> expected = parse(hotspot.output);
        // Bulk.end() prints "(end)". Without it the HotSpot run stopped early
        // -- a check that exits the JVM -- and every later check would be
        // missing from both sides and compare as equal.
        assertTrue(expected.containsKey("(end)"), "the HotSpot run never reached Bulk.end():\n"
                + tail(hotspot.output));
        Map<String, List<String>> actual = parse(parpar.output);
        compare(expected, actual, failures);
        BulkGeneratedCodeChecks.checkOutput(actual, failures);
        if (parpar.exit != 0) {
            String last = actual.isEmpty() ? "(none)" : new ArrayList<>(actual.keySet()).get(actual.size() - 1);
            failures.add(0, "ParparVM exited with " + parpar.exit + " during check " + last + ":\n"
                    + tail(parpar.output));
        }
        int checks = expected.size() - (expected.containsKey("(end)") ? 1 : 0);
        System.out.println("[bulk] " + checks + " checks in " + groups.size() + " groups; "
                + failures.size() + " failing");
        if (!failures.isEmpty()) {
            fail(failures.size() + " bulk check(s) differ between HotSpot and ParparVM (outputs under "
                    + work + "):\n" + String.join("\n", failures));
        }
    }

    /// Splits a run's output into checks: a `### name` line opens one, every
    /// line up to the next belongs to it. A runtime diagnostic -- an upper
    /// case tag in brackets, `[GC-INSTR] ...` -- is never a check's output and
    /// is dropped on both sides. Only that shape: dropping every line that
    /// starts with `[` silently stopped comparing a check that printed an
    /// array or a list on a line of its own.
    static Map<String, List<String>> parse(String output) {
        Map<String, List<String>> checks = new LinkedHashMap<>();
        List<String> current = null;
        for (String line : output.split("\\R")) {
            if (line.startsWith("### ")) {
                current = new ArrayList<>();
                checks.put(line.substring(4), current);
            } else if (current != null && !DIAGNOSTIC.matcher(line).lookingAt()) {
                current.add(line);
            }
        }
        return checks;
    }

    static void compare(Map<String, List<String>> expected, Map<String, List<String>> actual, List<String> failures) {
        Set<String> names = new LinkedHashSet<>(expected.keySet());
        names.addAll(actual.keySet());
        for (String name : names) {
            List<String> e = expected.get(name);
            List<String> a = actual.get(name);
            if (e == null) {
                failures.add(name + ": ran on ParparVM only");
                continue;
            }
            if (a == null) {
                failures.add(name + ": never ran on ParparVM");
                continue;
            }
            // "Both runtimes printed the same" is only evidence when something
            // was printed and every line of the check ran. A check that prints
            // nothing compares nothing, and one that throws out of its body on
            // HotSpot stops at the throw on both sides -- its remaining
            // assertions then pass on ParparVM without running there. Both
            // passed silently; neither is ever intended.
            if (e.isEmpty()) {
                failures.add(name + ": printed nothing on HotSpot, so it compares nothing");
                continue;
            }
            for (String line : e) {
                if (line.startsWith("THREW ")) {
                    failures.add(name + ": threw out of its body on HotSpot (" + clip(line)
                            + "); the rest of the check never ran. Catch the expected exception"
                            + " with Bulk.pThrown");
                }
            }
            if (e.get(0).startsWith("SKIPPED ")) {
                // The JDK's answer differs from Codename One's here, so there is
                // nothing to compare -- but ParparVM still runs the body, and an
                // uncaught exception there is a failure, not a difference.
                for (String line : a) {
                    if (line.startsWith("THREW ")) {
                        failures.add(name + " (skipped on HotSpot): ParparVM " + clip(line));
                    }
                }
                continue;
            }
            if (e.size() == 1 && "CN1-ONLY".equals(e.get(0))) {
                for (String line : a) {
                    if (line.startsWith("FAIL") || line.startsWith("THREW")) {
                        failures.add(name + " (ParparVM only): " + line);
                    }
                }
                continue;
            }
            if (e.equals(a)) {
                continue;
            }
            int i = 0;
            while (i < e.size() && i < a.size() && e.get(i).equals(a.get(i))) {
                i++;
            }
            failures.add(name + ": line " + (i + 1) + "\n    HotSpot:  " + clip(i < e.size() ? e.get(i) : "(no line)")
                    + "\n    ParparVM: " + clip(i < a.size() ? a.get(i) : "(no line)"));
        }
    }

    private static String clip(String s) {
        if (s.length() <= 600) {
            return s;
        }
        // A RESULT line of a folded fixture carries every case; show where it
        // starts to differ rather than its first kilobyte.
        return s.substring(0, 600) + "...";
    }

    /// A runtime diagnostic line: `[TAG]` or `[TAG-SUB]` at the start.
    static final java.util.regex.Pattern DIAGNOSTIC = java.util.regex.Pattern.compile("\\[[A-Z][A-Z0-9_-]*\\]");

    /// Core classes compiled into the bulk program, from `CodenameOne/src`
    /// itself: the pure-Java digests a built application's host answers
    /// `MessageDigest` with, and the `com.codename1.crypto` stack over them, so
    /// the program runs the same code an application does rather than a copy. All are
    /// `@SharedWithBackend`, i.e. depend on the JDK subset alone. Each is in
    /// the `paths` of `.github/workflows/parparvm-tests.yml`.
    static final String[] CORE_SOURCES = {
        "com/codename1/impl/SharedWithBackend.java",
        "com/codename1/io/CharArrayReader.java",
        "com/codename1/io/JSONParseCallback.java",
        "com/codename1/io/JSONParser.java",
        "com/codename1/io/JSONSanitizer.java",
        "com/codename1/crypto/UInt256.java",
        "com/codename1/util/BigInteger.java",
        "com/codename1/util/MathUtil.java",
        "com/codename1/util/TBigInteger.java",
        "com/codename1/util/TBitLevel.java",
        "com/codename1/util/TConversion.java",
        "com/codename1/util/TDivision.java",
        "com/codename1/util/TElementary.java",
        "com/codename1/util/TLogical.java",
        "com/codename1/util/TMultiplication.java",
        "com/codename1/util/TPrimality.java",
        "com/codename1/crypto/bitcoin/BitcoinAddress.java",
        "com/codename1/crypto/bitcoin/BitcoinKey.java",
        "com/codename1/crypto/bitcoin/BitcoinNetwork.java",
        "com/codename1/crypto/bitcoin/BitcoinTransaction.java",
        "com/codename1/crypto/bitcoin/Der.java",
        "com/codename1/crypto/bitcoin/Psbt.java",
        "com/codename1/crypto/bitcoin/Script.java",
        "com/codename1/crypto/bitcoin/Taproot.java",
        "com/codename1/crypto/bitcoin/Wif.java",
        "com/codename1/crypto/bitcoin/Wire.java",
        "com/codename1/crypto/encoding/Base58.java",
        "com/codename1/crypto/encoding/Base58Check.java",
        "com/codename1/crypto/encoding/Base64.java",
        "com/codename1/crypto/encoding/Bech32.java",
        "com/codename1/crypto/encoding/Hex.java",
        "com/codename1/crypto/encoding/Rlp.java",
        "com/codename1/crypto/evm/Abi.java",
        "com/codename1/crypto/evm/Erc20.java",
        "com/codename1/crypto/evm/EvmAddress.java",
        "com/codename1/crypto/evm/EvmTransaction.java",
        "com/codename1/crypto/evm/PersonalSign.java",
        "com/codename1/crypto/evm/TypedData.java",
        "com/codename1/crypto/hd/Bip32.java",
        "com/codename1/crypto/hd/Bip39English.java",
        "com/codename1/crypto/hd/DerivationPath.java",
        "com/codename1/crypto/hd/Mnemonic.java",
        "com/codename1/crypto/hd/Slip10.java",
        "com/codename1/crypto/market/CachingPriceFeed.java",
        "com/codename1/crypto/market/CoinGeckoFeed.java",
        "com/codename1/crypto/market/JsonPriceFeed.java",
        "com/codename1/crypto/market/PriceFeed.java",
        "com/codename1/crypto/market/PriceFeeds.java",
        "com/codename1/crypto/market/Quote.java",
        "com/codename1/crypto/rpc/EsploraClient.java",
        "com/codename1/crypto/rpc/EvmRpc.java",
        "com/codename1/crypto/rpc/JsonRpc.java",
        "com/codename1/crypto/rpc/RpcException.java",
        "com/codename1/crypto/rpc/RpcTransport.java",
        "com/codename1/crypto/rpc/SolanaRpc.java",
        "com/codename1/crypto/solana/AccountMeta.java",
        "com/codename1/crypto/solana/ComputeBudget.java",
        "com/codename1/crypto/solana/Instruction.java",
        "com/codename1/crypto/solana/ShortVec.java",
        "com/codename1/crypto/solana/SolanaAddress.java",
        "com/codename1/crypto/solana/SolanaMessage.java",
        "com/codename1/crypto/solana/SolanaTransaction.java",
        "com/codename1/crypto/solana/SystemProgram.java",
        "com/codename1/crypto/solana/TokenProgram.java",
        "com/codename1/crypto/wallet/SeedSigner.java",
        "com/codename1/crypto/wallet/Signer.java",
        "com/codename1/crypto/wallet/Wallet.java",
        "com/codename1/security/Base32.java",
        "com/codename1/security/CryptoBatch.java",
        "com/codename1/security/CryptoEngine.java",
        "com/codename1/security/CryptoException.java",
        "com/codename1/security/Ed25519.java",
        "com/codename1/security/Ed25519Math.java",
        "com/codename1/security/Hash.java",
        "com/codename1/security/Hkdf.java",
        "com/codename1/security/Hmac.java",
        "com/codename1/security/MessageDigestImpl.java",
        "com/codename1/security/NativeCryptoEngine.java",
        "com/codename1/security/NativeCryptoKernels.java",
        "com/codename1/security/Otp.java",
        "com/codename1/security/Pbkdf2.java",
        "com/codename1/security/Schnorr.java",
        "com/codename1/security/Secp256k1.java",
        "com/codename1/security/Secp256k1Math.java",
    };

    /// `CodenameOne/src`: `-Dbulk.repo` names the checkout, otherwise it is
    /// found above the working directory (vm/tests under Maven).
    static Path coreSources() {
        String repo = System.getProperty("bulk.repo");
        if (repo != null && !repo.trim().isEmpty()) {
            return Paths.get(repo.trim(), "CodenameOne", "src");
        }
        for (Path p = Paths.get("").toAbsolutePath(); p != null; p = p.getParent()) {
            Path src = p.resolve("CodenameOne").resolve("src");
            if (Files.isRegularFile(src.resolve(CORE_SOURCES[0]))) {
                return src;
            }
        }
        fail("CodenameOne/src not found above " + Paths.get("").toAbsolutePath() + "; pass -Dbulk.repo");
        return null;
    }

    static Path bulkDir() throws URISyntaxException {
        URL url = BulkConformanceIntegrationTest.class.getResource(BULK + "/Bulk.java");
        assertNotNull(url, "bulk program resources should be on the test class path");
        return Paths.get(url.toURI()).getParent();
    }

    static List<String> groups(Path bulkDir) throws IOException {
        String only = System.getProperty("bulk.groups");
        Set<String> filter = null;
        if (only != null && !only.trim().isEmpty()) {
            filter = new LinkedHashSet<>(Arrays.asList(only.trim().split("\\s*,\\s*")));
        }
        List<String> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(bulkDir)) {
            for (Path p : files.sorted().collect(Collectors.toList())) {
                String f = p.getFileName().toString();
                if (f.endsWith("Checks.java")) {
                    String cls = f.substring(0, f.length() - ".java".length());
                    if (filter == null || filter.contains(cls)) {
                        out.add(cls);
                    }
                }
            }
        }
        if (filter != null) {
            assertEquals(filter.size(), out.size(), "-Dbulk.groups names a group that does not exist: " + filter);
        }
        return out;
    }

    /// With `-Dbulk.groups`, only the harness and the named groups compile: a
    /// group `XChecks` owns every file whose name starts with `X`.
    static boolean belongsTo(String file, List<String> groups) {
        if (file.startsWith("Bulk")) {
            return true;
        }
        for (String g : groups) {
            if (file.startsWith(g.substring(0, g.length() - "Checks".length()))) {
                return true;
            }
        }
        return false;
    }

    static String mainSource(List<String> groups) {
        StringBuilder sb = new StringBuilder();
        sb.append("/// Generated by BulkConformanceIntegrationTest.\n");
        sb.append("public class BulkApp {\n");
        sb.append("    public static void main(String[] args) throws Exception {\n");
        sb.append("        Bulk.begin();\n");
        for (String g : groups) {
            sb.append("        ").append(g).append(".run();\n");
        }
        sb.append("        Bulk.end();\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    /// `BulkResources.get(name)`: every file under `res`, as the bytes the
    /// HotSpot run reads from its class path.
    static String resourcesSource(Path res) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("/// Generated by BulkConformanceIntegrationTest from bulk/res.\n");
        sb.append("final class BulkResources {\n");
        sb.append("    static byte[] get(String name) {\n");
        if (Files.isDirectory(res)) {
            List<Path> files;
            try (Stream<Path> all = Files.walk(res)) {
                files = all.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
            }
            for (Path f : files) {
                String name = res.relativize(f).toString().replace('\\', '/');
                sb.append("        if (\"").append(name).append("\".equals(name)) {\n");
                sb.append("            return bytes(\"").append(latin1Literal(Files.readAllBytes(f))).append("\");\n");
                sb.append("        }\n");
            }
        }
        sb.append("        return null;\n");
        sb.append("    }\n\n");
        sb.append("    private static byte[] bytes(String s) {\n");
        sb.append("        byte[] b = new byte[s.length()];\n");
        sb.append("        for (int i = 0; i < b.length; i++) {\n");
        sb.append("            b[i] = (byte) s.charAt(i);\n");
        sb.append("        }\n");
        sb.append("        return b;\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String latin1Literal(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length);
        for (byte b : data) {
            int c = b & 0xff;
            if (c >= 0x20 && c < 0x7f && c != '"' && c != '\\') {
                sb.append((char) c);
            } else {
                // Octal, not a unicode escape: javac turns a unicode escape into
                // its character before it lexes, so a newline written that way
                // ends the string literal.
                sb.append(String.format("\\%03o", c));
            }
        }
        return sb.toString();
    }

    static final class Run {
        final int exit;
        final String output;

        Run(int exit, String output) {
            this.exit = exit;
            this.output = output;
        }
    }

    /// Runs a command to completion and keeps its output whatever the exit
    /// status: a crash part-way through still tells which checks ran.
    static Run run(List<String> command, Path dir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        Path log = dir.resolve("output.log");
        pb.redirectOutput(log.toFile());
        System.out.println("Running command: " + String.join(" ", command) + " (output: " + log + ")");
        Process p = pb.start();
        p.getOutputStream().close();
        boolean finished = p.waitFor(20, TimeUnit.MINUTES);
        if (!finished) {
            p.destroyForcibly();
            p.waitFor(10, TimeUnit.SECONDS);
        }
        String output = new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
        return new Run(finished ? p.exitValue() : -1, output);
    }

    private static String tail(String output) {
        String[] lines = output.split("\\R");
        int from = Math.max(0, lines.length - 40);
        return String.join("\n", Arrays.asList(lines).subList(from, lines.length));
    }

    private static CompilerHelper.CompilerConfig selectCompiler() {
        // The checks use Java 21 API (SwitchoverMembersApp calls the sequenced
        // List methods), so prefer 21; 17 is the last resort for the rest.
        for (String target : new String[] {"21", "25", "17"}) {
            for (CompilerHelper.CompilerConfig config : CompilerHelper.getAvailableCompilers(target)) {
                if (CompilerHelper.isJavaApiCompatible(config)) {
                    return config;
                }
            }
        }
        return null;
    }

    private static Path referenceJdk(CompilerHelper.CompilerConfig fallback) {
        for (String v : new String[] {"21", "25", "19"}) {
            for (CompilerHelper.CompilerConfig candidate : CompilerHelper.getAvailableCompilers(v)) {
                if (CompilerHelper.getJdkMajor(candidate) >= 19) {
                    return candidate.jdkHome;
                }
            }
        }
        return fallback.jdkHome;
    }
}
