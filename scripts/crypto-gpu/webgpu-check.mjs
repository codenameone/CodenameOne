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
 * Runs the WebGPU kernels of Ports/JavaScriptPort/src/main/webapp/js/cn1_crypto_gpu.js in headless
 * Chromium and holds every answer to the C build of the same kernels:
 *
 *   scripts/crypto-gpu/vectors.sh 48 > vectors.txt
 *   scripts/crypto-gpu/vectors.sh tables > tables.txt
 *   scripts/crypto-gpu/vectors.sh hashes > hashes.txt
 *   node scripts/crypto-gpu/webgpu-check.mjs vectors.txt tables.txt hashes.txt [--bench N]
 *
 * Playwright is resolved from PLAYWRIGHT_MODULE, or from the usual module path, and the browser
 * from CHROMIUM_PATH when that is set. Exits 0 when every
 * answer matches, 1 on a mismatch, 2 when the browser has no WebGPU adapter.
 */
import fs from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const require = createRequire(import.meta.url);
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");

const args = process.argv.slice(2);
const benchAt = args.indexOf("--bench");
const bench = benchAt >= 0 ? parseInt(args.splice(benchAt, 2)[1], 10) : 0;
const [vectorsFile, tablesFile, hashesFile] = args;
const shaders = fs.readFileSync(path.join(here, "../../Ports/JavaScriptPort/src/main/webapp/js/cn1_crypto_gpu.js"), "utf8");

const kernels = { 2: "ecdsa", 3: "schnorr", 4: "ed25519" };
const records = { ecdsa: [], schnorr: [], ed25519: [] };
for (const line of fs.readFileSync(vectorsFile, "utf8").split("\n")) {
    const f = line.trim().split(" ");
    if (f.length === 7 && f[4] === "2") {
        records[kernels[f[0]]].push({ rec: f[5], answer: parseInt(f[6], 10) });
    }
}
const tables = {};
for (const line of fs.readFileSync(tablesFile, "utf8").split("\n")) {
    const f = line.trim().split(" ");
    if (f.length === 2) {
        tables[f[0]] = f[1];
    }
}
const hashes = fs.readFileSync(hashesFile, "utf8").split("\n").filter(l => l.trim()).map(l => {
    const f = l.trim().split(" ");
    return { len: parseInt(f[0], 10), msg: f[1] === "-" ? "" : f[1], sha256: f[2], keccak256: f[3] };
});

async function page() {
    /* everything below runs in the browser */
    const W = CN1_CRYPTO_GPU_WGSL;
    const words = hex => {
        const w = new Uint32Array(hex.length / 8);
        for (let i = 0; i < w.length; i++) {
            w[i] = parseInt(hex.substr(i * 8, 8), 16);
        }
        return w;
    };
    const bytes = hex => {
        const b = new Uint8Array(hex.length / 2);
        for (let i = 0; i < b.length; i++) {
            b[i] = parseInt(hex.substr(i * 2, 2), 16);
        }
        return b;
    };
    const toHex = b => Array.from(b, x => x.toString(16).padStart(2, "0")).join("");
    const adapter = navigator.gpu && await navigator.gpu.requestAdapter();
    if (!adapter) {
        return { noGpu: true };
    }
    const device = await adapter.requestDevice();
    const pipes = {};
    const pipe = async k => {
        if (!pipes[k]) {
            const t0 = performance.now();
            const module = device.createShaderModule({ code: W[k] });
            pipes[k] = await device.createComputePipelineAsync({ layout: "auto", compute: { module, entryPoint: "main" } });
            pipes[k].ms = performance.now() - t0;
        }
        return pipes[k];
    };
    const buf = (data, usage) => {
        const size = Math.max(16, (data.byteLength + 3) & ~3);
        const b = device.createBuffer({ size, usage, mappedAtCreation: true });
        new Uint8Array(b.getMappedRange()).set(new Uint8Array(data.buffer, data.byteOffset, data.byteLength));
        b.unmap();
        return b;
    };
    /* the tables the port uploads are the ones in cn1_crypto_gpu.js; they must be the C hosts' */
    const shipped = {};
    for (const t of ["g", "b"]) {
        const raw = atob(CN1_CRYPTO_GPU_TABLES[t]);
        const b = new Uint8Array(raw.length);
        for (let i = 0; i < raw.length; i++) {
            b[i] = raw.charCodeAt(i);
        }
        shipped[t] = new Uint32Array(b.buffer);
        if (Array.from(shipped[t], x => x.toString(16).padStart(8, "0")).join("") !== DATA.tables[t]) {
            return { checked: 0, bad: ["table " + t + " in cn1_crypto_gpu.js differs from the C build's"] };
        }
    }
    const tbl = { g: buf(shipped.g, GPUBufferUsage.STORAGE), b: buf(shipped.b, GPUBufferUsage.STORAGE) };
    const run = async (k, input, len, count, outWords, table) => {
        const p = await pipe(k);
        const inB = buf(input, GPUBufferUsage.STORAGE);
        const outB = device.createBuffer({ size: Math.max(16, outWords * 4), usage: GPUBufferUsage.STORAGE | GPUBufferUsage.COPY_SRC });
        const read = device.createBuffer({ size: Math.max(16, outWords * 4), usage: GPUBufferUsage.MAP_READ | GPUBufferUsage.COPY_DST });
        const uni = buf(new Uint32Array([len, count, 0, 0]), GPUBufferUsage.UNIFORM);
        const entries = [{ binding: 0, resource: { buffer: inB } }, { binding: 1, resource: { buffer: outB } },
            { binding: 3, resource: { buffer: uni } }];
        if (table) {
            /* a layout: "auto" pipeline has only the bindings its kernel reads, and the hashes read no table */
            entries.push({ binding: 2, resource: { buffer: table } });
        }
        const group = device.createBindGroup({ layout: p.getBindGroupLayout(0), entries });
        const enc = device.createCommandEncoder();
        const pass = enc.beginComputePass();
        pass.setPipeline(p);
        pass.setBindGroup(0, group);
        pass.dispatchWorkgroups(Math.ceil(count / 64));
        pass.end();
        enc.copyBufferToBuffer(outB, 0, read, 0, Math.max(16, outWords * 4));
        device.queue.submit([enc.finish()]);
        await read.mapAsync(GPUMapMode.READ);
        const out = new Uint32Array(read.getMappedRange().slice(0, outWords * 4));
        read.unmap();
        [inB, outB, read, uni].forEach(b => b.destroy());
        return out;
    };
    const report = { checked: 0, bad: [], compileMs: {}, bench: {}, adapter: (adapter.info && adapter.info.description) || "" };
    const recWords = { ecdsa: 49, schnorr: 40, ed25519: 40 };
    for (const k of ["ecdsa", "schnorr", "ed25519"]) {
        const rs = DATA.records[k];
        const input = words(rs.map(r => r.rec).join(""));
        const out = await run(k, input, 0, rs.length, rs.length, k === "ed25519" ? tbl.b : tbl.g);
        rs.forEach((r, i) => {
            report.checked++;
            if ((out[i] !== 0 ? 1 : 0) !== r.answer) {
                report.bad.push(k + " #" + i + " answered " + out[i] + ", C answered " + r.answer);
            }
        });
        if (DATA.bench > 0) {
            const n = DATA.bench, big = new Uint32Array(n * recWords[k]);
            for (let i = 0; i < n; i++) {
                big.set(input.subarray((i % rs.length) * recWords[k], (i % rs.length + 1) * recWords[k]), i * recWords[k]);
            }
            let best = 1e9;
            for (let rep = 0; rep < 3; rep++) {
                const t0 = performance.now();
                await run(k, big, 0, n, n, k === "ed25519" ? tbl.b : tbl.g);
                best = Math.min(best, performance.now() - t0);
            }
            report.bench[k] = Math.round(n / best * 1000);
        }
    }
    for (const k of ["sha256", "keccak256"]) {
        for (const h of DATA.hashes) {
            const entries = h.len === 0 ? 16 : h.msg.length / 2 / h.len;
            const out = await run(k, h.len === 0 ? new Uint8Array(4) : bytes(h.msg), h.len, entries, entries * 8, null);
            const got = toHex(new Uint8Array(out.buffer));
            report.checked += entries;
            if (got !== h[k]) {
                report.bad.push(k + " at length " + h.len + " answered " + got.substr(0, 64) + ", C answered " + h[k].substr(0, 64));
            }
        }
    }
    for (const k in pipes) {
        report.compileMs[k] = Math.round(pipes[k].ms);
    }
    return report;
}

const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH || undefined, args: ["--enable-unsafe-webgpu", "--enable-features=Vulkan,WebGPU", "--use-angle=metal"] });
try {
    const p = await browser.newPage();
    /* WebGPU needs a secure context: serve an empty page from localhost */
    await p.route("http://localhost/**", r => r.fulfill({ contentType: "text/html", body: "<!doctype html><title>gpu</title>" }));
    await p.goto("http://localhost/");
    await p.addScriptTag({ content: shaders });
    await p.addScriptTag({ content: "var DATA = " + JSON.stringify({ records, tables, hashes, bench }) + ";" });
    const report = await p.evaluate("(" + page.toString() + ")()");
    if (report.noGpu) {
        console.log("no WebGPU adapter in this browser");
        process.exitCode = 2;
    }
    else { console.log(JSON.stringify(report)); process.exitCode = report.bad.length === 0 && report.checked > 0 ? 0 : 1; }
} finally {
    await browser.close();
}
