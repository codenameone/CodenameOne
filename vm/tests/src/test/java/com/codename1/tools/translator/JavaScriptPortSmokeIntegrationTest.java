package com.codename1.tools.translator;

import org.junit.jupiter.params.ParameterizedTest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaScriptPortSmokeIntegrationTest {
    private static final Path REPO_ROOT = Paths.get("..", "..").normalize();
    private static final Path FIXTURE_ROOT = REPO_ROOT.resolve(Paths.get("Ports", "JavaScriptPort", "tests", "fixtures"));
    private static final Path LICENSE_ROOT = REPO_ROOT.resolve(Paths.get("Ports", "JavaScriptPort"));
    private static final Path HOST_BRIDGE_SOURCE = REPO_ROOT.resolve(Paths.get("Ports", "JavaScriptPort", "src", "main", "java",
            "com", "codename1", "impl", "platform", "js", "JavaScriptPortHost.java"));

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void executesJavaScriptPortSmokeFixtureThroughParparVmHostBridge(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path classesDir = Files.createTempDirectory("js-port-smoke-classes");
        Path javaApiDir = Files.createTempDirectory("js-port-smoke-javaapi");
        Path hostBridgeDir = classesDir.resolveSibling("js-port-smoke-host-source");

        Files.createDirectories(hostBridgeDir.resolve(Paths.get("com", "codename1", "impl", "platform", "js")));
        Files.write(hostBridgeDir.resolve(Paths.get("com", "codename1", "impl", "platform", "js", "JavaScriptPortHost.java")),
                Files.readAllBytes(HOST_BRIDGE_SOURCE));
        Files.write(hostBridgeDir.resolve("JavaScriptPortSmokeApp.java"),
                Files.readAllBytes(FIXTURE_ROOT.resolve("JavaScriptPortSmokeApp.java")));

        JavascriptTargetIntegrationTest.compileAgainstJavaApi(config, hostBridgeDir, classesDir, javaApiDir);

        Path outputDir = Files.createTempDirectory("js-port-smoke-output");
        JavascriptTargetIntegrationTest.runJavascriptTranslator(classesDir, outputDir, "JavaScriptPortSmokeApp");

        Path distDir = outputDir.resolve("dist").resolve("JavaScriptPortSmokeApp-js");
        assertTrue(Files.exists(distDir.resolve("worker.js")), "Translator should emit a worker bootstrap for the JavaScript port smoke fixture");
        assertTrue(Files.exists(distDir.resolve("translated_app.js")), "Translator should emit translated classes for the JavaScript port smoke fixture");

        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);
        assertTrue(translatedApp.contains("cn1_com_codename1_impl_platform_js_JavaScriptPortHost_bootstrap_int_R_int"),
                "Smoke fixture should retain the JavaScript port host bridge symbol names");

        WorkerRunResult result = runGeneratedWorkerBundleWithJavaScriptPortHost(distDir);
        assertEquals("result", result.type, "Generated worker bundle should complete via the ParparVM worker protocol");
        assertEquals(930, result.result, "Smoke fixture should preserve the JavaScript port host contract end-to-end");
        assertTrue(result.errorMessage == null || result.errorMessage.isEmpty(), "Worker should not emit an error message");
    }

    @org.junit.jupiter.api.Test
    void javascriptPortBoundaryUsesPolyformHeaders() throws Exception {
        try (Stream<Path> paths = Files.walk(LICENSE_ROOT)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java") || path.toString().endsWith(".md"))
                    .forEach(path -> {
                        try {
                            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                            if (path.toString().endsWith(".java")) {
                                assertTrue(text.contains("PolyForm Noncommercial License 1.0.0"),
                                        "Missing PolyForm header in " + path);
                            }
                            assertFalse(text.contains("Classpath exception"), "Inherited parent license header found in " + path);
                            assertFalse(text.contains("GNU General Public License"), "Inherited GPL text found in " + path);
                            assertFalse(text.contains("DO NOT ALTER OR REMOVE COPYRIGHT NOTICES"), "Inherited Oracle/CN1 header found in " + path);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        }
    }

    private static WorkerRunResult runGeneratedWorkerBundleWithJavaScriptPortHost(Path distDir) throws Exception {
        Path harness = Files.createTempFile("js-port-smoke-host", ".js");
        Files.write(harness, generatedWorkerHarnessSourceWithJavaScriptPortHost(distDir).getBytes(StandardCharsets.UTF_8));
        Process process = new ProcessBuilder("node", harness.toString()).start();
        String output = readAll(process.getInputStream());
        String errors = readAll(process.getErrorStream());
        int rc = process.waitFor();
        assertEquals(0, rc, "Node worker harness should exit cleanly. stdout: " + output + " stderr: " + errors);
        WorkerRunResult out = new WorkerRunResult();
        out.rawMessage = output.trim();
        out.type = extractJsonString(output, "type");
        String result = extractJsonNumber(output, "result");
        out.result = result == null ? Integer.MIN_VALUE : Integer.parseInt(result);
        out.errorMessage = extractJsonString(output, "message");
        return out;
    }

    private static String generatedWorkerHarnessSourceWithJavaScriptPortHost(Path distDir) {
        return ""
                + "const fs = require('fs');\n"
                + "const path = require('path');\n"
                + "const { Worker } = require('worker_threads');\n"
                + "const bootstrapPath = path.join(" + quoteJs(distDir.toString()) + ", '__node_worker_bootstrap_jsport.js');\n"
                + "fs.writeFileSync(bootstrapPath, `\n"
                + "const fs = require('fs');\n"
                + "const path = require('path');\n"
                + "const vm = require('vm');\n"
                + "const { parentPort, workerData } = require('worker_threads');\n"
                + "global.self = global;\n"
                + "global.window = global;\n"
                + "global.global = global;\n"
                + "global.postMessage = function(msg) { parentPort.postMessage(msg); };\n"
                + "global.importScripts = function() {\n"
                + "  for (const script of arguments) {\n"
                + "    const scriptPath = path.join(workerData.distDir, String(script));\n"
                + "    const src = fs.readFileSync(scriptPath, 'utf8');\n"
                + "    vm.runInThisContext(src, { filename: scriptPath });\n"
                + "  }\n"
                + "};\n"
                + "parentPort.on('message', function(data) {\n"
                + "  if (typeof self.onmessage === 'function') {\n"
                + "    self.onmessage({ data: data });\n"
                + "  }\n"
                + "});\n"
                + "const workerSrc = fs.readFileSync(path.join(workerData.distDir, 'worker.js'), 'utf8');\n"
                + "vm.runInThisContext(workerSrc, { filename: path.join(workerData.distDir, 'worker.js') });\n"
                + "`);\n"
                + "const storage = new Map();\n"
                + "const database = new Map();\n"
                + "let mediaPlaying = false;\n"
                + "let pointerCount = 0;\n"
                + "const worker = new Worker(bootstrapPath, { workerData: { distDir: " + quoteJs(distDir.toString()) + " } });\n"
                + "let done = false;\n"
                + "worker.on('message', function(msg) {\n"
                + "  if (done) {\n"
                + "    return;\n"
                + "  }\n"
                + "  if (msg && msg.type === 'host-call') {\n"
                + "    const symbol = msg.symbol;\n"
                + "    const args = msg.args || [];\n"
                + "    let value;\n"
                + "    if (symbol === 'cn1_com_codename1_impl_platform_js_JavaScriptPortHost_bootstrap_int_R_int') {\n"
                + "      value = (args[0] | 0) === 1 ? 101 : -1;\n"
                + "    } else if (symbol === 'cn1_com_codename1_impl_platform_js_JavaScriptPortHost_resourceThemeChecksum_int_R_int') {\n"
                + "      value = (args[0] | 0) === 7 ? 103 : -2;\n"
                + "    } else if (symbol === 'cn1_com_codename1_impl_platform_js_JavaScriptPortHost_networkFetchStatus_int_R_int') {\n"
                + "      value = (args[0] | 0) === 11 ? 107 : -3;\n"
                + "    } else if (symbol === 'cn1_com_codename1_impl_platform_js_JavaScriptPortHost_storageWriteRead_int_int_R_int') {\n"
                + "      storage.set(args[0] | 0, args[1] | 0);\n"
                + "      value = storage.get(args[0] | 0) | 0;\n"
                + "    } else if (symbol === 'cn1_com_codename1_impl_platform_js_JavaScriptPortHost_databaseWriteRead_int_int_R_int') {\n"
                + "      database.set(args[0] | 0, args[1] | 0);\n"
                + "      value = database.get(args[0] | 0) | 0;\n"
                + "    } else if (symbol === 'cn1_com_codename1_impl_platform_js_JavaScriptPortHost_browserNavigateAndEval_int_R_int') {\n"
                + "      value = (args[0] | 0) === 13 ? 127 : -4;\n"
                + "    } else if (symbol === 'cn1_com_codename1_impl_platform_js_JavaScriptPortHost_mediaPlayAndQuery_int_R_int') {\n"
                + "      mediaPlaying = (args[0] | 0) === 17;\n"
                + "      value = mediaPlaying ? 131 : -5;\n"
                + "    } else if (symbol === 'cn1_com_codename1_impl_platform_js_JavaScriptPortHost_dispatchPointer_int_int_R_int') {\n"
                + "      if ((args[0] | 0) === 19 && (args[1] | 0) === 23) {\n"
                + "        pointerCount++;\n"
                + "      }\n"
                + "      value = pointerCount > 0 ? 139 : -6;\n"
                + "    } else {\n"
                + "      worker.postMessage({ type: 'host-callback', id: msg.id, error: true, errorMessage: 'Unexpected host call ' + symbol });\n"
                + "      return;\n"
                + "    }\n"
                + "    worker.postMessage({ type: 'host-callback', id: msg.id, value: value });\n"
                + "    return;\n"
                + "  }\n"
                + "  if (msg && (msg.type === 'result' || msg.type === 'error')) {\n"
                + "    done = true;\n"
                + "    console.log(JSON.stringify(msg));\n"
                + "    worker.terminate().then(function() { process.exit(0); });\n"
                + "  }\n"
                + "});\n"
                + "worker.on('error', function(err) {\n"
                + "  if (done) {\n"
                + "    return;\n"
                + "  }\n"
                + "  done = true;\n"
                + "  console.log(JSON.stringify({ type: 'error', message: String(err) }));\n"
                + "  process.exit(1);\n"
                + "});\n"
                + "worker.postMessage({ type: 'start' });\n"
                + "setTimeout(function() {\n"
                + "  if (!done) {\n"
                + "    console.log(JSON.stringify({ type: 'error', message: 'Timed out waiting for JavaScript port smoke result' }));\n"
                + "    worker.terminate().then(function() { process.exit(1); });\n"
                + "  }\n"
                + "}, 10000);\n";
    }

    private static String quoteJs(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private static String readAll(InputStream input) throws Exception {
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > -1) {
                out.write(buffer, 0, len);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String extractJsonString(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = json.indexOf('"', start);
        return end < 0 ? null : json.substring(start, end);
    }

    private static String extractJsonNumber(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = start;
        while (end < json.length()) {
            char ch = json.charAt(end);
            if ((ch < '0' || ch > '9') && ch != '-') {
                break;
            }
            end++;
        }
        return json.substring(start, end);
    }

    private static final class WorkerRunResult {
        String rawMessage;
        String type;
        int result;
        String errorMessage;
    }
}
