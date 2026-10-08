#!/usr/bin/env node
import assert from "node:assert/strict";
import http from "node:http";
import { after, before, test } from "node:test";
import { chromium } from "playwright";
import { checkRenderedPage, diagramRenderError } from "./validate_mermaid.mjs";

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
test("rejects a page that lost a diagram", async () => {
  await assert.rejects(checkRenderedPage(page, `${base}/valid`, 2), /count changed/);
});
test("rejects unrendered source and invisible SVGs", async () => {
  await page.setContent('<div id="source">flowchart LR; A --> B;</div><div id="hidden"><svg style="display:none"></svg></div>');
  const errors = await page.evaluate(checkSource => {
    const check = (0, eval)(`(${checkSource})`);
    return [check(document.querySelector("#source")), check(document.querySelector("#hidden"))];
  }, diagramRenderError.toString());
  assert.deepEqual(errors, ["No rendered SVG", "Rendered SVG has no visible dimensions"]);
});
