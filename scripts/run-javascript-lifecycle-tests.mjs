#!/usr/bin/env node
//
// Playwright-driven smoke test for JavaScript-port bundles.
//
// Loads each requested bundle in headless Chromium, polls for the
// translator-emitted lifecycle milestones, and asserts that the app
// reaches both ``window.cn1Initialized = true`` and
// ``window.cn1Started = true`` within a per-bundle timeout. When a
// milestone is missed, the report includes the most recent
// ``PARPAR:DIAG:FIRST_FAILURE`` and ``PARPAR-LIFECYCLE`` console
// lines so the failure mode is visible without trawling the full
// browser log.
//
// This test is the regression harness for the long fa4247a42 →
// efd4eb3cf chain that took the Initializr bundle from "stuck on
// Loading… with nothing in the console" to a clean boot. Concrete
// regressions it catches:
//
//   * Runtime missing PARPAR-LIFECYCLE markers entirely (the
//     ``vmLifecycle`` always-on console lines were the only signal
//     the user had during the original loading hang).
//   * ``cn1Initialized`` set but ``cn1Started`` never set (lifecycle
//     hangs inside ``Lifecycle.start``).
//   * ``__parparError`` populated by the runtime — covers
//     ``Missing virtual method`` / ``Missing JS member`` / yield-in-
//     non-generator failures we kept hitting.
//
// Usage:
//   node scripts/run-javascript-lifecycle-tests.mjs [bundle.zip|dir]+
//
// With no arguments, defaults to the two CI-relevant bundles:
//   scripts/hellocodenameone/parparvm/target/hellocodenameone-javascript-port.zip
//   scripts/initializr/javascript/target/initializr-javascript-port.zip
//
// Environment:
//   CN1_LIFECYCLE_TIMEOUT_SECONDS  per-bundle timeout (default 90s)
//   CN1_LIFECYCLE_REPORT_DIR       artifacts directory; per-bundle
//                                  browser logs and report.json land here

import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawn, spawnSync as nodeSpawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

let chromium;
try {
  ({ chromium } = await import('playwright'));
} catch (playwrightError) {
  try {
    ({ chromium } = await import('@playwright/test'));
  } catch (playwrightTestError) {
    console.error('Unable to load Playwright. Install either "playwright" or "@playwright/test".');
    console.error('  Import from "playwright" failed:', String(playwrightError));
    console.error('  Import from "@playwright/test" failed:', String(playwrightTestError));
    process.exit(2);
  }
}

const TIMEOUT_SECONDS = Number(process.env.CN1_LIFECYCLE_TIMEOUT_SECONDS || '90');
const REPO_ROOT = path.resolve(__dirname, '..');
const REPORT_DIR = process.env.CN1_LIFECYCLE_REPORT_DIR
    || path.join(REPO_ROOT, 'artifacts', 'javascript-lifecycle-tests');
const HARNESS_PY = path.join(__dirname, 'javascript_browser_harness.py');

fs.mkdirSync(REPORT_DIR, { recursive: true });

const DEFAULT_BUNDLES = [
  {
    name: 'hellocodenameone',
    bundle: path.join(REPO_ROOT, 'scripts', 'hellocodenameone', 'parparvm', 'target', 'hellocodenameone-javascript-port.zip')
  },
  {
    name: 'initializr',
    bundle: path.join(REPO_ROOT, 'scripts', 'initializr', 'javascript', 'target', 'initializr-javascript-port.zip')
  }
];

function parseArgs(argv) {
  if (argv.length === 0) {
    return DEFAULT_BUNDLES;
  }
  return argv.map((arg, idx) => ({
    name: path.basename(arg).replace(/\.(zip|war|jar)$/i, '') || `bundle-${idx}`,
    bundle: path.resolve(arg)
  }));
}

/**
 * Materialise a bundle into a freshly-created directory so the harness
 * server can serve it. Accepts either a directory (copied) or one of
 * the bundled archive shapes (zip / war / jar — unzip-extractable).
 */
function materializeBundle(input, dest) {
  fs.mkdirSync(dest, { recursive: true });
  const stat = fs.statSync(input);
  if (stat.isDirectory()) {
    copyTree(input, dest);
    return;
  }
  const ext = path.extname(input).toLowerCase();
  if (ext === '.zip' || ext === '.war' || ext === '.jar') {
    const result = nodeSpawnSync('unzip', ['-qq', input, '-d', dest], { encoding: 'utf8' });
    if (result.status !== 0) {
      throw new Error(`unzip failed for ${input}: status=${result.status} stderr=${result.stderr}`);
    }
    return;
  }
  throw new Error(`Unsupported bundle input: ${input}`);
}

function copyTree(src, dst) {
  fs.mkdirSync(dst, { recursive: true });
  for (const entry of fs.readdirSync(src, { withFileTypes: true })) {
    const sp = path.join(src, entry.name);
    const dp = path.join(dst, entry.name);
    if (entry.isDirectory()) {
      copyTree(sp, dp);
    } else if (entry.isSymbolicLink()) {
      fs.symlinkSync(fs.readlinkSync(sp), dp);
    } else {
      fs.copyFileSync(sp, dp);
    }
  }
}

/**
 * Inject the harness server's log-capture probe (the same shim
 * ``run-javascript-browser-tests.sh`` uses for the screenshot
 * suite) into ``index.html``. Without it, console output never
 * makes it into ``browser.log`` — Playwright still sees the
 * messages directly via ``page.on('console')`` so the test still
 * works, but the saved log artifact stays empty and the failure
 * report has nothing for a CI consumer to grep through.
 */
function injectProbeScript(indexHtml) {
  if (!fs.existsSync(indexHtml)) return;
  let text = fs.readFileSync(indexHtml, 'utf8');
  const probe = '<script src="/__cn1__/probe.js"></script>';
  if (text.indexOf(probe) >= 0) return;
  const bridge = '<script src="browser_bridge.js"></script>';
  if (text.indexOf(bridge) >= 0) {
    text = text.replace(bridge, probe + '\n' + bridge);
  } else if (text.indexOf('</body>') >= 0) {
    text = text.replace('</body>', probe + '\n</body>');
  } else {
    text += '\n' + probe + '\n';
  }
  fs.writeFileSync(indexHtml, text, 'utf8');
}

/**
 * Walk into the bundle directory and return the directory containing
 * ``index.html``. Bundle archives wrap the actual content in a
 * single ``<App>-js/`` folder, so we have to descend.
 */
function locateIndexRoot(root) {
  if (fs.existsSync(path.join(root, 'index.html'))) {
    return root;
  }
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    if (!entry.isDirectory()) continue;
    const sub = path.join(root, entry.name);
    const found = locateIndexRoot(sub);
    if (found) return found;
  }
  return null;
}

/**
 * Spawn ``javascript_browser_harness.py`` to serve the bundle on a
 * loopback port. Returns once the harness has written its URL to the
 * url-file; that's our handshake that the listener is up.
 */
async function startHarness(serveDir, logFile, urlFile) {
  fs.writeFileSync(urlFile, '');
  const child = spawn('python3', [
    HARNESS_PY,
    '--serve-dir', serveDir,
    '--log-file', logFile,
    '--url-file', urlFile
  ], { stdio: ['ignore', 'pipe', 'pipe'] });
  child.stdout.on('data', () => {});
  child.stderr.on('data', () => {});
  for (let i = 0; i < 100; i++) {
    if (fs.statSync(urlFile).size > 0) {
      const url = fs.readFileSync(urlFile, 'utf8').trim();
      if (url) {
        return { child, url };
      }
    }
    await new Promise(r => setTimeout(r, 50));
  }
  child.kill('SIGTERM');
  throw new Error('Harness did not announce a URL within 5 seconds');
}

/**
 * Append parparDiag=1 and cn1DisableEventForwarding=1 (mirrors the
 * screenshot-test harness so the lifecycle log we read is identical
 * to the one CI captures).
 */
function decorateUrl(url) {
  const sep = url.includes('?') ? '&' : '?';
  return `${url}${sep}parparDiag=1&cn1DisableEventForwarding=1`;
}

/**
 * Drive a single bundle through the lifecycle test. Returns a result
 * record; never throws on bundle-side failure (those become
 * ``ok: false``). Throws only on infrastructure issues (harness
 * failed to start, Chromium failed to launch).
 */
async function runBundle({ name, bundle }) {
  const workDir = fs.mkdtempSync(path.join(os.tmpdir(), `cn1-lifecycle-${name}-`));
  const serveDir = path.join(workDir, 'served');
  const bundleDir = path.join(workDir, 'bundle');
  const logFile = path.join(workDir, 'browser.log');
  const urlFile = path.join(workDir, 'url.txt');

  console.log(`[lifecycle] ${name}: materialising ${bundle}`);
  materializeBundle(bundle, bundleDir);
  const indexRoot = locateIndexRoot(bundleDir);
  if (!indexRoot) {
    return { name, bundle, ok: false, milestones: {}, reason: 'bundle has no index.html' };
  }
  copyTree(indexRoot, serveDir);
  injectProbeScript(path.join(serveDir, 'index.html'));

  console.log(`[lifecycle] ${name}: starting harness`);
  let harness;
  try {
    harness = await startHarness(serveDir, logFile, urlFile);
  } catch (err) {
    return { name, bundle, ok: false, milestones: {}, reason: `harness start failed: ${err.message}` };
  }

  const url = decorateUrl(harness.url);
  console.log(`[lifecycle] ${name}: serving at ${url}`);

  // Capture every lifecycle marker and the most-recent FIRST_FAILURE
  // so the report can pinpoint where the bundle stalled.
  const lifecycle = [];
  let firstFailure = null;
  let pageError = null;

  const browser = await chromium.launch({
    headless: true,
    args: [
      '--autoplay-policy=no-user-gesture-required',
      '--disable-web-security',
      '--allow-file-access-from-files'
    ]
  });

  let result;
  try {
    const page = await browser.newPage({
      viewport: { width: 375, height: 667 },
      deviceScaleFactor: 2
    });

    page.on('console', msg => {
      const text = msg.text();
      if (text.indexOf('PARPAR-LIFECYCLE:') >= 0) {
        lifecycle.push(text);
      }
      if (text.indexOf('PARPAR:DIAG:FIRST_FAILURE') >= 0) {
        // Aggregate into a single record; the runtime emits
        // ``category`` / ``methodId`` / ``receiverClass`` as separate
        // lines. Last value wins, which is fine since the runtime
        // only updates ``__parparError`` once per failure burst.
        const match = text.match(/PARPAR:DIAG:FIRST_FAILURE:(\w+)=(.+)$/);
        if (match) {
          firstFailure = firstFailure || {};
          firstFailure[match[1]] = match[2];
        }
      }
    });
    page.on('pageerror', err => { pageError = String(err && err.stack || err); });

    await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 30000 });

    const milestones = await pollLifecycle(page, TIMEOUT_SECONDS);
    result = {
      name,
      bundle,
      ok: milestones.cn1Initialized && milestones.cn1Started && !pageError,
      milestones,
      lifecycle,
      firstFailure,
      pageError
    };
  } finally {
    try { await browser.close(); } catch (_e) {}
    try { harness.child.kill('SIGTERM'); } catch (_e) {}
  }

  // Persist the captured browser log alongside the structured report
  // so a CI consumer can dig in without reproducing the run locally.
  try {
    fs.copyFileSync(logFile, path.join(REPORT_DIR, `${name}.browser.log`));
  } catch (_e) {}
  return result;
}

/**
 * Poll the page for ``cn1Initialized`` and ``cn1Started`` flags
 * (set by ParparVMBootstrap.setInitialized / setStarted at the end
 * of ``Lifecycle.init`` and ``Lifecycle.start`` respectively). A
 * ``__parparError`` short-circuits the wait so a runtime exception
 * is reported promptly instead of running out the full timeout.
 */
async function pollLifecycle(page, timeoutSeconds) {
  const deadline = Date.now() + timeoutSeconds * 1000;
  let cn1Initialized = false;
  let cn1Started = false;
  let parparError = null;

  while (Date.now() < deadline) {
    const state = await page.evaluate(() => ({
      initialized: !!window.cn1Initialized,
      started: !!window.cn1Started,
      error: window.__parparError ? JSON.stringify(window.__parparError) : ''
    }));
    if (state.initialized) cn1Initialized = true;
    if (state.started) cn1Started = true;
    if (state.error) parparError = state.error;
    if (cn1Started || parparError) {
      break;
    }
    await new Promise(r => setTimeout(r, 500));
  }
  return { cn1Initialized, cn1Started, parparError };
}

function summarise(results) {
  console.log('');
  console.log('==== Lifecycle test results ====');
  let failed = 0;
  for (const r of results) {
    const status = r.ok ? 'OK' : 'FAIL';
    console.log(`[${status}] ${r.name} (${path.basename(r.bundle)})`);
    if (!r.ok) {
      failed++;
      if (r.reason) {
        console.log(`       reason: ${r.reason}`);
      }
      if (r.milestones) {
        console.log(`       milestones: cn1Initialized=${r.milestones.cn1Initialized} cn1Started=${r.milestones.cn1Started}`);
        if (r.milestones.parparError) {
          console.log(`       __parparError: ${r.milestones.parparError.substring(0, 300)}`);
        }
      }
      if (r.firstFailure) {
        console.log(`       FIRST_FAILURE: ${JSON.stringify(r.firstFailure)}`);
      }
      if (r.pageError) {
        console.log(`       pageerror: ${r.pageError.substring(0, 300)}`);
      }
      if (r.lifecycle && r.lifecycle.length) {
        console.log(`       last lifecycle markers:`);
        for (const line of r.lifecycle.slice(-6)) {
          console.log(`         ${line}`);
        }
      } else {
        console.log(`       (no PARPAR-LIFECYCLE markers — runtime never produced one)`);
      }
    }
  }
  return failed;
}

async function main() {
  const bundles = parseArgs(process.argv.slice(2));
  const results = [];
  for (const b of bundles) {
    if (!fs.existsSync(b.bundle)) {
      results.push({ name: b.name, bundle: b.bundle, ok: false, milestones: {}, reason: `bundle does not exist: ${b.bundle}` });
      continue;
    }
    try {
      results.push(await runBundle(b));
    } catch (err) {
      results.push({
        name: b.name,
        bundle: b.bundle,
        ok: false,
        milestones: {},
        reason: `infrastructure error: ${err && err.stack || err}`
      });
    }
  }

  fs.writeFileSync(path.join(REPORT_DIR, 'report.json'), JSON.stringify(results, null, 2));
  const failed = summarise(results);
  if (failed > 0) {
    console.log('');
    console.log(`${failed}/${results.length} bundle(s) failed lifecycle test`);
    process.exit(1);
  }
}

await main();
