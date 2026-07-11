#!/usr/bin/env node

import { spawn } from "node:child_process";
import { mkdir, mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, resolve } from "node:path";
import { pathToFileURL } from "node:url";

const args = parseArgs(process.argv.slice(2));
const html = resolve(args.html || `${process.env.HOME}/Downloads/Codename One Settings.html`);
const output = resolve(args.output || "scripts/settings/target/reference.png");
const metricsOutput = resolve(args.metrics || output.replace(/\.png$/i, ".json"));
const width = positiveInt(args.width, 1470);
const height = positiveInt(args.height, 612);
const scale = positiveNumber(args.scale, 2);
const section = (args.section || "basic").toLowerCase();
const dark = args.theme !== "light";
const menuOpen = args.menu === "true" || args.menu === true;
const chrome = args.chrome || "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

const profile = await mkdtemp(`${tmpdir()}/cn1-settings-reference-`);
let child;
let cdp;

try {
    const browserWs = await launchChrome();
    cdp = await connect(browserWs);
    const { targetId } = await cdp.send("Target.createTarget", { url: "about:blank" });
    const { sessionId } = await cdp.send("Target.attachToTarget", { targetId, flatten: true });

    await cdp.send("Page.enable", {}, sessionId);
    await cdp.send("Runtime.enable", {}, sessionId);
    await cdp.send("Emulation.setDeviceMetricsOverride", {
        width,
        height,
        deviceScaleFactor: scale,
        mobile: false,
        screenWidth: width,
        screenHeight: height
    }, sessionId);

    await cdp.send("Page.navigate", { url: pathToFileURL(html).href }, sessionId);
    await waitForApp(sessionId);
    await evaluate(sessionId, `localStorage.setItem('cn1settings_theme', ${JSON.stringify(dark ? "dark" : "light")}); location.reload()`);
    await waitForApp(sessionId);

    if (section !== "basic") {
        const label = section === "extensions" ? "Ext" : section === "hints" ? "Hints" : section;
        await evaluate(sessionId, `(() => {
            const item = [...document.querySelectorAll('.railitem,.navitem')]
                .find(node => node.textContent.trim() === ${JSON.stringify(label)});
            if (!item) throw new Error('Section control not found: ${label}');
            item.click();
        })()`);
        await waitForSelector(sessionId, section === "extensions" ? ".extgrid" : ".hintgroup");
    }

    if (menuOpen) {
        await evaluate(sessionId, `(() => {
            const buttons = [...document.querySelectorAll('.topbar .iconbtn')];
            if (!buttons.length) throw new Error('Toolbar menu control not found');
            buttons[buttons.length - 1].click();
        })()`);
        await waitForSelector(sessionId, ".menu");
    }

    const metrics = await evaluate(sessionId, `(() => {
        const selectors = ['.app','.topbar','.brand','.mark','.appname','.pathchip','.topbar .btn','.topbar .iconbtn','.menu','.rail','.railitem','.content','.cwrap','.h1','.sub','.grid2','.field','.input','.icondrop','.searchbox','.extgrid','.extcard','.hintcard'];
        const result = { viewport: { width: innerWidth, height: innerHeight, scale: devicePixelRatio }, theme: document.querySelector('.app')?.dataset.theme || document.querySelector('[data-theme]')?.dataset.theme };
        for (const selector of selectors) {
            result[selector] = [...document.querySelectorAll(selector)].map(node => {
                const r = node.getBoundingClientRect();
                const style = getComputedStyle(node);
                return {
                    text: node.textContent.trim().replace(/\\s+/g, ' ').slice(0, 120),
                    x: round(r.x), y: round(r.y), width: round(r.width), height: round(r.height),
                    color: style.color, background: style.backgroundColor, border: style.borderColor,
                    fontFamily: style.fontFamily, fontSize: style.fontSize, fontWeight: style.fontWeight,
                    padding: style.padding, gap: style.gap
                };
            });
        }
        return result;
        function round(value) { return Math.round(value * 100) / 100; }
    })()`);

    const shot = await cdp.send("Page.captureScreenshot", { format: "png", fromSurface: true }, sessionId);
    await mkdir(dirname(output), { recursive: true });
    await writeFile(output, Buffer.from(shot.data, "base64"));
    await writeFile(metricsOutput, `${JSON.stringify(metrics, null, 2)}\n`);
    console.log(`Captured ${output} (${width}x${height} @${scale}x)`);
    console.log(`Measured ${metricsOutput}`);
} finally {
    cdp?.close();
    child?.kill("SIGTERM");
    await rm(profile, { recursive: true, force: true });
}

async function launchChrome() {
    child = spawn(chrome, [
        "--headless=new",
        "--disable-gpu",
        "--disable-background-networking",
        "--disable-component-update",
        "--disable-default-apps",
        "--disable-features=Translate,MediaRouter",
        "--hide-scrollbars",
        "--no-first-run",
        "--no-default-browser-check",
        "--remote-debugging-port=0",
        `--user-data-dir=${profile}`,
        "about:blank"
    ], { stdio: ["ignore", "ignore", "pipe"] });

    return new Promise((resolveWs, rejectWs) => {
        let stderr = "";
        const timeout = setTimeout(() => rejectWs(new Error(`Chrome did not expose DevTools. ${stderr}`)), 15000);
        child.stderr.setEncoding("utf8");
        child.stderr.on("data", chunk => {
            stderr += chunk;
            const match = stderr.match(/DevTools listening on (ws:\/\/[^\s]+)/);
            if (match) {
                clearTimeout(timeout);
                resolveWs(match[1]);
            }
        });
        child.once("exit", code => {
            clearTimeout(timeout);
            rejectWs(new Error(`Chrome exited before capture (code ${code}). ${stderr}`));
        });
    });
}

async function connect(url) {
    const ws = new WebSocket(url);
    const pending = new Map();
    let nextId = 1;
    await new Promise((resolveOpen, rejectOpen) => {
        ws.addEventListener("open", resolveOpen, { once: true });
        ws.addEventListener("error", rejectOpen, { once: true });
    });
    ws.addEventListener("message", event => {
        const message = JSON.parse(String(event.data));
        if (!message.id) return;
        const waiter = pending.get(message.id);
        if (!waiter) return;
        pending.delete(message.id);
        if (message.error) waiter.reject(new Error(`${waiter.method}: ${message.error.message}`));
        else waiter.resolve(message.result || {});
    });
    return {
        send(method, params = {}, sessionId) {
            return new Promise((resolveSend, rejectSend) => {
                const id = nextId++;
                pending.set(id, { resolve: resolveSend, reject: rejectSend, method });
                ws.send(JSON.stringify({ id, method, params, ...(sessionId ? { sessionId } : {}) }));
            });
        },
        close() { ws.close(); }
    };
}

async function waitForApp(sessionId) {
    await waitForSelector(sessionId, ".app");
    await waitFor(sessionId, `document.fonts.status === 'loaded'`, 10000);
    await delay(150);
}

async function waitForSelector(sessionId, selector) {
    await waitFor(sessionId, `document.querySelector(${JSON.stringify(selector)}) !== null`, 15000);
}

async function waitFor(sessionId, expression, timeoutMs) {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
        if (await evaluate(sessionId, expression)) return;
        await delay(50);
    }
    throw new Error(`Timed out waiting for: ${expression}`);
}

async function evaluate(sessionId, expression) {
    const response = await cdp.send("Runtime.evaluate", {
        expression,
        awaitPromise: true,
        returnByValue: true
    }, sessionId);
    if (response.exceptionDetails) {
        throw new Error(response.exceptionDetails.exception?.description || response.exceptionDetails.text);
    }
    return response.result?.value;
}

function delay(ms) {
    return new Promise(resolveDelay => setTimeout(resolveDelay, ms));
}

function positiveInt(value, fallback) {
    const parsed = Number.parseInt(value, 10);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function positiveNumber(value, fallback) {
    const parsed = Number(value);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function parseArgs(values) {
    const parsed = {};
    for (let i = 0; i < values.length; i++) {
        const key = values[i].replace(/^--/, "");
        const next = values[i + 1];
        if (!values[i].startsWith("--")) continue;
        if (next && !next.startsWith("--")) {
            parsed[key] = next;
            i++;
        } else {
            parsed[key] = true;
        }
    }
    return parsed;
}
