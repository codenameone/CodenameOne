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

/*
 * Serves a translated JavaScriptCryptoBatchApp (js-batch-check.sh builds it) and runs it in
 * headless Chromium with WebGPU:
 *
 *   node scripts/crypto-gpu/js-batch-check.mjs <dist-dir> [entries]
 *
 * Prints the app's CN1-CRYPTO lines and fails unless every kernel's GPU answers agree with the
 * portable engine's. Exits 2 when the browser has no WebGPU adapter, since the app then only
 * measures the CPU against itself.
 */
import fs from "node:fs";
import http from "node:http";
import path from "node:path";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");
const root = path.resolve(process.argv[2]);
const entries = parseInt(process.argv[3] || "4096", 10);
const types = { ".js": "text/javascript", ".html": "text/html", ".css": "text/css", ".wasm": "application/wasm" };
const server = http.createServer((req, res) => {
    const file = path.resolve(root, "." + (req.url.split("?")[0] === "/" ? "/index.html" : req.url.split("?")[0]));
    if (!file.startsWith(root + path.sep)) {
        res.writeHead(403).end();
        return;
    }
    fs.readFile(file, (error, data) => {
        res.writeHead(error ? 404 : 200, { "Content-Type": types[path.extname(file)] || "application/octet-stream" });
        res.end(error ? "Not found" : data);
    });
});
await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH || undefined,
    args: ["--enable-unsafe-webgpu", "--enable-features=Vulkan,WebGPU", "--use-angle=metal"] });
const lines = [];
let failed = false;
// runs the app once; failGpu > 0 makes that many GPU dispatches fail, so the batches the GPU was
// given finish on the Java path and must still agree
async function run(failGpu) {
    const page = await browser.newPage();
    const done = new Promise(resolve => {
        page.on("console", msg => {
            const text = msg.text();
            if (text.startsWith("CN1-CRYPTO")) {
                console.log(text);
                lines.push(text);
                if (text === "CN1-CRYPTO-DONE") {
                    resolve();
                }
            }
        });
    });
    page.on("pageerror", e => console.log("page error: " + e));
    lines.length = 0;
    await page.goto("http://127.0.0.1:" + server.address().port + "/?n=" + entries
        + (failGpu > 0 ? "&cn1CryptoGpuFail=" + failGpu : ""));
    const hasGpu = await page.evaluate(async () => !!(navigator.gpu && await navigator.gpu.requestAdapter()));
    if (!hasGpu) {
        console.log("no WebGPU adapter in this browser");
        process.exitCode = 2;
    } else {
        await Promise.race([done, new Promise((_, reject) => setTimeout(() => reject(new Error("timed out")), 1800000))]);
        const kernels = lines.filter(l => /^CN1-CRYPTO \w+ /.test(l));
        for (const l of kernels) {
            const m = l.match(/agree=(\d+)\/(\d+)/);
            if (!m || m[1] !== m[2]) {
                failed = true;
            }
        }
        if (kernels.length !== 5 || lines.some(l => l.startsWith("CN1-CRYPTO-ERROR"))) {
            failed = true;
        }
        process.exitCode = failed ? 1 : 0;
    }
    await page.close();
}
try {
    await run(0);
    if (process.exitCode === 0) {
        console.log("-- again, with the first three GPU dispatches failing");
        await run(3);
    }
} finally {
    await browser.close();
    server.closeAllConnections();
    server.close();
}
