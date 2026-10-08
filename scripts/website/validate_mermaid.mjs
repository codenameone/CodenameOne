#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import http from "node:http";
import { pathToFileURL } from "node:url";
import { chromium, firefox } from "playwright";

const MERMAID_RE = /\{\{[<%]\s*mermaid\b[^%>]*[%>]\}\}([\s\S]*?)\{\{[<%]\s*\/\s*mermaid\s*[%>]\}\}/g;
const MERMAID_URL = "https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs";

export function diagramRenderError(node) {
  const svg = node.querySelector("svg");
  if (!svg) return "No rendered SVG";
  // Mermaid's error display is itself an SVG. Counting SVGs is not validation.
  if (svg.querySelector(".error-icon, .error-text") ||
      /Syntax error in text|mermaid version/i.test(svg.textContent.replace(/<[^>]*>/g, ""))) {
    return "Mermaid rendered an error diagram";
  }
  const box = svg.getBoundingClientRect();
  if (box.width <= 0 || box.height <= 0) return "Rendered SVG has no visible dimensions";
  return null;
}

function collectFiles(input, extension) {
  const fullPath = path.resolve(input);
  if (!fs.statSync(fullPath).isDirectory()) return fullPath.endsWith(extension) ? [fullPath] : [];
  return fs.readdirSync(fullPath, { withFileTypes: true }).flatMap(entry => {
    const child = path.join(fullPath, entry.name);
    // Do not follow symlinks into generated assets or back out of the site.
    if (entry.isDirectory()) return collectFiles(child, extension);
    return entry.isFile() && entry.name.endsWith(extension) ? [child] : [];
  });
}

function extractDiagrams(file) {
  const text = fs.readFileSync(file, "utf8");
  return [...text.matchAll(MERMAID_RE)].map(match => ({
    file,
    line: text.slice(0, match.index).split(/\r?\n/).length,
    source: match[1].trim(),
  }));
}

export function collectSources(inputs, site) {
  const files = [...new Set(inputs.flatMap(input => collectFiles(input, ".md")))].sort();
  const diagrams = [];
  const renderedFiles = [];
  const outputs = new Set();
  for (const file of files) {
    const extracted = extractDiagrams(file);
    diagrams.push(...extracted);
    if (!site || !extracted.length) continue;
    // Blog posts declare their public URL explicitly. Use that source contract,
    // never the generated HTML: a broken shortcode can remove every container.
    const text = fs.readFileSync(file, "utf8");
    const frontmatter = text.match(/^---\r?\n([\s\S]*?)\r?\n---(?:\r?\n|$)/)?.[1];
    const url = frontmatter?.match(/^url:\s*(?:"([^"\r\n]+)"|'([^'\r\n]+)'|([^\s#"']+))\s*(?:#.*)?$/m);
    const route = url && (url[1] || url[2] || url[3]);
    if (!route || !route.startsWith("/") || route.startsWith("//") || /[?#\\]/.test(route)) {
      throw new Error(`${file}: Mermaid page needs an explicit site-relative frontmatter url`);
    }
    const output = path.resolve(site, `.${route}`, route.endsWith(".html") ? "" : "index.html");
    if (!output.startsWith(path.resolve(site) + path.sep)) throw new Error(`${file}: url escapes --site output`);
    if (outputs.has(output)) throw new Error(`${file}: duplicate Mermaid page URL ${route}`);
    outputs.add(output);
    renderedFiles.push({ file: output, source: file, count: extracted.length });
  }
  return { diagrams, renderedFiles };
}

export async function checkRenderedPage(page, url, expectedCount) {
  const response = await page.goto(url, { waitUntil: "domcontentloaded" });
  if (!response?.ok()) throw new Error(`Page returned HTTP ${response?.status()}`);
  const nodes = page.locator(".cn1-mermaid");
  const actualCount = await nodes.count();
  if (actualCount !== expectedCount) throw new Error(`Generated diagram count changed: expected ${expectedCount}, found ${actualCount}`);
  await page.waitForFunction(() => Boolean(window.__cn1MermaidRender), null, { timeout: 30000 });
  // An empty SVG is inserted before async layout. Wait for the actual loader,
  // including any rejection, rather than racing it with DOM existence checks.
  await page.evaluate(() => Promise.race([
    window.__cn1MermaidRender,
    new Promise((_, reject) => setTimeout(() => reject(new Error("Mermaid render timed out")), 30000)),
  ]));
  const failures = await nodes.evaluateAll((nodes, checkSource) => {
    const check = (0, eval)(`(${checkSource})`);
    return nodes.map((node, i) => ({ diagram: i + 1, error: check(node) })).filter(x => x.error);
  }, diagramRenderError.toString());
  if (failures.length) throw new Error(JSON.stringify(failures));
}

export async function serveSite(root) {
  const types = { ".html": "text/html", ".js": "text/javascript", ".css": "text/css", ".svg": "image/svg+xml", ".png": "image/png", ".jpg": "image/jpeg", ".woff2": "font/woff2" };
  const server = http.createServer((req, res) => {
    try {
      const urlPath = decodeURIComponent(new URL(req.url, "http://localhost").pathname);
      let file = path.resolve(root, `.${urlPath}`);
      if (file !== root && !file.startsWith(root + path.sep)) { res.writeHead(403).end(); return; }
      if (fs.statSync(file).isDirectory()) file = path.join(file, "index.html");
      res.setHeader("Content-Type", types[path.extname(file)] || "application/octet-stream");
      fs.createReadStream(file).on("error", () => res.destroy()).pipe(res);
    } catch { res.writeHead(404).end(); }
  });
  await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
  return { server, base: `http://127.0.0.1:${server.address().port}` };
}

async function main(args) {
  const inputs = [];
  const engines = [];
  let site;
  while (args.length) {
    const arg = args.shift();
    if (arg === "--site") site = path.resolve(args.shift() || "");
    else if (arg === "--browser") engines.push(args.shift());
    else if (arg.startsWith("--")) throw new Error(`Unknown option: ${arg}`);
    else inputs.push(arg);
  }
  if (!inputs.length) throw new Error("Usage: node scripts/website/validate_mermaid.mjs [--site <Hugo output>] [--browser chromium|firefox] <markdown files or directories...>");
  if (!engines.length) engines.push("chromium");
  for (const engine of engines) if (!["chromium", "firefox"].includes(engine)) throw new Error(`Unknown browser: ${engine}`);
  const { diagrams, renderedFiles } = collectSources(inputs, site);
  if (!diagrams.length) { console.log("No Mermaid diagrams found."); return; }
  const served = site ? await serveSite(site) : null;
  const failures = [];
  try {
    for (const engine of engines) {
      const browser = await ({ chromium, firefox })[engine].launch({ headless: true });
      try {
        const page = await browser.newPage();
        // Avoid loading site analytics, discussion widgets or remote demo content.
        await page.route("**/*", route => {
          const url = new URL(route.request().url());
          return url.hostname === "127.0.0.1" || url.hostname === "cdn.jsdelivr.net" ? route.continue() : route.abort();
        });
        await page.setContent(`<script type="module">import mermaid from "${MERMAID_URL}"; mermaid.initialize({startOnLoad:false,securityLevel:"loose"}); window.__mermaid=mermaid;</script><div id="fixture"></div>`);
        await page.waitForFunction(() => window.__mermaid, null, { timeout: 30000 });
        for (const diagram of diagrams) {
          const error = await page.evaluate(async ({ source, checkSource }) => {
            try {
              await window.__mermaid.parse(source);
              const node = document.createElement("div");
              node.className = "mermaid";
              // Exercise the HTML-to-Mermaid path used by the shortcode as well as parse().
              node.innerHTML = source;
              document.querySelector("#fixture").replaceChildren(node);
              await window.__mermaid.run({ nodes: [node] });
              return (0, eval)(`(${checkSource})`)(node);
            } catch (err) { return err?.str || err?.message || String(err); }
          }, { source: diagram.source, checkSource: diagramRenderError.toString() });
          if (error) failures.push(`${engine}: ${diagram.file}:${diagram.line}\n${error}`);
        }
        for (const item of renderedFiles) {
          const relative = path.relative(site, item.file).split(path.sep).map(encodeURIComponent).join("/");
          try { await checkRenderedPage(page, `${served.base}/${relative}`, item.count); }
          catch (err) { failures.push(`${engine}: ${item.source} -> ${item.file}\n${err.message}`); }
        }
        console.log(`${engine}: checked ${diagrams.length} source renders and ${renderedFiles.length} generated pages.`);
      } finally { await browser.close(); }
    }
  } finally { if (served) await new Promise(resolve => served.server.close(resolve)); }
  if (failures.length) throw new Error(`Mermaid validation failed:\n\n${failures.join("\n\n")}`);
  console.log("Mermaid parse and render gate passed.");
}

if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
  main(process.argv.slice(2)).catch(err => { console.error(err.message); process.exitCode = 1; });
}
