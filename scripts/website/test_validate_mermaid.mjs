#!/usr/bin/env node
import assert from "node:assert/strict";
import http from "node:http";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { after, before, test } from "node:test";
import { chromium } from "playwright";
import { checkRenderedPage, collectSources, diagramRenderError, serveSite } from "./validate_mermaid.mjs";

let browser;
let page;
let server;
let base;
before(async () => {
  browser = await chromium.launch({ headless: true });
  page = await browser.newPage();
  server = http.createServer((req, res) => {
    res.setHeader("Content-Type", "text/html");
    // The failure has a perfectly valid SVG, just like Mermaid's error output.
    const content = req.url === "/error" ? '<text class="error-text">Syntax error in text</text>' : '<rect width="80" height="30"/>';
    if (req.url === "/late-error") {
      res.end(`<div class="cn1-mermaid"><svg width="100" height="50"></svg></div><script>
        window.__cn1MermaidRender = new Promise(resolve => setTimeout(() => {
          document.querySelector('svg').innerHTML = '<text class="error-text">Syntax error in text</text>';
          resolve();
        }, 150));
      </script>`);
    } else {
      res.end(`<div class="cn1-mermaid"><svg width="100" height="50">${content}</svg></div><script>window.__cn1MermaidRender = Promise.resolve();</script>`);
    }
  });
  await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
  base = `http://127.0.0.1:${server.address().port}`;
});
after(async () => {
  if (browser) await browser.close();
  if (server) await new Promise(resolve => server.close(resolve));
});

test("accepts a visible diagram in a generated page", async () => {
  await checkRenderedPage(page, `${base}/valid`, 1);
});
test("rejects Mermaid's error SVG in a generated page", async () => {
  await assert.rejects(checkRenderedPage(page, `${base}/error`, 1), /error diagram/);
});
test("waits for rendering before accepting an initially empty SVG", async () => {
  await assert.rejects(checkRenderedPage(page, `${base}/late-error`, 1), /error diagram/);
});
for (const scenario of [
  { name: "all diagrams present", count: 2 },
  { name: "one diagram lost", count: 1, error: /expected 2, found 1/ },
  { name: "all diagrams lost on one page", count: 0, error: /expected 2, found 0/ },
  { name: "whole output page missing", count: null, error: /HTTP 404/ },
]) {
  test(`checks Markdown expectations against generated files: ${scenario.name}`, async () => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "cn1-mermaid-"));
    const source = path.join(root, "source");
    const site = path.join(root, "site");
    fs.mkdirSync(source);
    fs.mkdirSync(path.join(site, "blog", "renamed-route"), { recursive: true });
    const diagram = '{{< mermaid >}}\nflowchart LR\nA --> B\n{{< /mermaid >}}\n';
    fs.writeFileSync(path.join(source, "different-filename.md"), `---\nurl: "/blog/renamed-route/"\n---\n${diagram.repeat(2)}`);
    fs.writeFileSync(path.join(source, "unchanged.md"), `---\nurl: /unchanged.html\n---\n${diagram}`);
    const html = count => '<div class="cn1-mermaid"><svg width="100" height="50"><rect width="80" height="30"/></svg></div>'.repeat(count)
      + '<script>window.__cn1MermaidRender = Promise.resolve();</script>';
    // A valid page remains, so losing all containers elsewhere must still fail.
    fs.writeFileSync(path.join(site, "unchanged.html"), html(1));
    if (scenario.count !== null) fs.writeFileSync(path.join(site, "blog", "renamed-route", "index.html"), html(scenario.count));
    const served = await serveSite(site);
    try {
      const { diagrams, renderedFiles } = collectSources([source], site);
      assert.equal(diagrams.length, 3);
      assert.equal(renderedFiles.length, 2);
      const failures = [];
      for (const item of renderedFiles) {
        const relative = path.relative(site, item.file).split(path.sep).map(encodeURIComponent).join("/");
        try { await checkRenderedPage(page, `${served.base}/${relative}`, item.count); }
        catch (err) { failures.push(err.message); }
      }
      if (scenario.error) {
        assert.equal(failures.length, 1);
        assert.match(failures[0], scenario.error);
      } else {
        assert.deepEqual(failures, []);
      }
    } finally {
      await new Promise(resolve => served.server.close(resolve));
      fs.rmSync(root, { recursive: true, force: true });
    }
  });
}
test("rejects unrendered source and invisible SVGs", async () => {
  await page.setContent('<div id="source">flowchart LR; A --> B;</div><div id="hidden"><svg style="display:none"></svg></div>');
  const errors = await page.evaluate(checkSource => {
    const check = (0, eval)(`(${checkSource})`);
    return [check(document.querySelector("#source")), check(document.querySelector("#hidden"))];
  }, diagramRenderError.toString());
  assert.deepEqual(errors, ["No rendered SVG", "Rendered SVG has no visible dimensions"]);
});
