#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import { chromium } from "playwright";

const MERMAID_RE = /\{\{[<%]\s*mermaid\b[^%>]*[%>]\}\}([\s\S]*?)\{\{[<%]\s*\/\s*mermaid\s*[%>]\}\}/g;

function extractDiagrams(file) {
  const text = fs.readFileSync(file, "utf8");
  const diagrams = [];
  let match;
  while ((match = MERMAID_RE.exec(text)) !== null) {
    const before = text.slice(0, match.index);
    diagrams.push({
      file,
      line: before.split(/\r?\n/).length,
      source: match[1].trim(),
    });
  }
  return diagrams;
}

function collectMarkdownFiles(input) {
  const fullPath = path.resolve(input);
  const stat = fs.statSync(fullPath);
  if (stat.isDirectory()) {
    const files = [];
    for (const entry of fs.readdirSync(fullPath, { withFileTypes: true })) {
      const child = path.join(fullPath, entry.name);
      if (entry.isDirectory()) {
        files.push(...collectMarkdownFiles(child));
      } else if (entry.isFile() && entry.name.endsWith(".md")) {
        files.push(child);
      }
    }
    return files;
  }
  return fullPath.endsWith(".md") ? [fullPath] : [];
}

const inputs = process.argv.slice(2);
if (inputs.length === 0) {
  console.error("Usage: node scripts/website/validate_mermaid.mjs <markdown files or directories...>");
  process.exit(2);
}

const files = [...new Set(inputs.flatMap(collectMarkdownFiles))].sort();
const diagrams = files.flatMap(extractDiagrams);
if (diagrams.length === 0) {
  console.log("No Mermaid diagrams found.");
  process.exit(0);
}

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();
await page.setContent(`
<!doctype html>
<meta charset="utf-8">
<script type="module">
  import mermaid from "https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs";
  mermaid.initialize({ startOnLoad: false, securityLevel: "loose" });
  window.__mermaid = mermaid;
  window.__mermaidReady = true;
</script>
`);
await page.waitForFunction("window.__mermaidReady === true", null, { timeout: 30000 });

const failures = [];
for (const diagram of diagrams) {
  const error = await page.evaluate(async (source) => {
    try {
      await window.__mermaid.parse(source);
      return null;
    } catch (err) {
      return err?.str || err?.message || String(err);
    }
  }, diagram.source);
  if (error) {
    failures.push({ ...diagram, error });
  }
}

await browser.close();

if (failures.length > 0) {
  console.error(`Mermaid validation failed for ${failures.length} diagram(s):`);
  for (const failure of failures) {
    console.error(`\n${failure.file}:${failure.line}`);
    console.error(failure.error);
  }
  process.exit(1);
}

console.log(`Validated ${diagrams.length} Mermaid diagram(s).`);
