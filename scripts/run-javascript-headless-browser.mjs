import fs from 'node:fs';

let chromium;
try {
  ({ chromium } = await import('playwright'));
} catch (playwrightError) {
  try {
    ({ chromium } = await import('@playwright/test'));
  } catch (playwrightTestError) {
    console.error('Unable to load Playwright. Install either "playwright" or "@playwright/test".');
    console.error('Import from "playwright" failed:', String(playwrightError));
    console.error('Import from "@playwright/test" failed:', String(playwrightTestError));
    process.exit(2);
  }
}

const url = process.env.URL;
const logFile = process.env.LOG_FILE;
const timeoutSeconds = Number(process.env.CN1_JS_BROWSER_LIFETIME_SECONDS || '120');

if (!url) {
  console.error('URL is required');
  process.exit(2);
}

const SUITE_FINISHED_MARKER = 'CN1SS:SUITE:FINISHED';

let suiteFinished = false;

function append(line) {
  const text = `[playwright] ${line}\n`;
  if (logFile) {
    fs.appendFileSync(logFile, text, 'utf8');
  } else {
    process.stdout.write(text);
  }
}

const profileWorker = process.env.CN1_JS_PROFILE_WORKER === '1';
const remoteDebugPort = Number(process.env.CN1_JS_CDP_PORT || '9242');
let profiler = null;
let profileFinalized = false;
let finalizeProfile = async () => {};

const launchArgs = [
  '--autoplay-policy=no-user-gesture-required',
  '--disable-web-security',
  '--allow-file-access-from-files'
];
if (profileWorker) {
  launchArgs.push(`--remote-debugging-port=${remoteDebugPort}`);
}

const browser = await chromium.launch({
  headless: true,
  args: launchArgs
});

// Raw CDP client over a single WebSocket. Playwright's newCDPSession() only
// accepts a Page/Frame, so it cannot reach a dedicated worker target. With a
// remote-debugging port + flatten:true we multiplex every session over one
// socket via the `sessionId` field and can drive Profiler.* on the wedged VM
// worker (V8 samples it out-of-process even while it spins synchronously).
async function startWorkerProfiler() {
  const ver = await (await fetch(`http://127.0.0.1:${remoteDebugPort}/json/version`)).json();
  const ws = new WebSocket(ver.webSocketDebuggerUrl);
  await new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });

  let nextId = 1;
  const pending = new Map();
  let workerSessionId = null;
  const ready = { resolve: null };
  const workerReady = new Promise(r => { ready.resolve = r; });

  ws.onmessage = (ev) => {
    const msg = JSON.parse(ev.data);
    if (msg.id && pending.has(msg.id)) {
      const { resolve, reject } = pending.get(msg.id);
      pending.delete(msg.id);
      msg.error ? reject(new Error(JSON.stringify(msg.error))) : resolve(msg.result);
      return;
    }
    if (msg.method === 'Target.attachedToTarget') {
      const t = msg.params.targetInfo;
      if (t.type === 'page') {
        send(msg.params.sessionId, 'Target.setAutoAttach',
          { autoAttach: true, flatten: true, waitForDebuggerOnStart: false });
      } else if ((t.type === 'worker' || t.type === 'dedicated_worker') && !workerSessionId) {
        workerSessionId = msg.params.sessionId;
        ready.resolve();
      }
    }
  };

  function send(sessionId, method, params) {
    const id = nextId++;
    const payload = { id, method, params: params || {} };
    if (sessionId) payload.sessionId = sessionId;
    return new Promise((resolve, reject) => {
      pending.set(id, { resolve, reject });
      ws.send(JSON.stringify(payload));
    });
  }

  await send(null, 'Target.setAutoAttach',
    { autoAttach: true, flatten: true, waitForDebuggerOnStart: false });
  append(`profiler:cdp-connected port=${remoteDebugPort}`);

  return {
    async begin() {
      await workerReady;
      await send(workerSessionId, 'Profiler.enable');
      await send(workerSessionId, 'Profiler.setSamplingInterval', { interval: 150 });
      await send(workerSessionId, 'Profiler.start');
      append(`profiler:started workerSession=${workerSessionId}`);
    },
    async stop() {
      if (!workerSessionId) return null;
      const { profile } = await send(workerSessionId, 'Profiler.stop');
      return profile;
    },
    close() { try { ws.close(); } catch { /* ignore */ } }
  };
}

try {
  const page = await browser.newPage({
    viewport: { width: 375, height: 667 },
    // deviceScaleFactor=2 emulates a retina display so window.devicePixelRatio
    // reports 2 — without it Chromium reports 1 and CN1 picks DENSITY_MEDIUM,
    // which leaves padding/margin/font sizes about half of what an iOS/Android
    // reference screenshot shows. Backing store is 750x1334 (similar area to
    // the previous 1280x900 viewport; still comfortably smaller per-test).
    deviceScaleFactor: 2
  });

  page.on('console', msg => {
    const text = msg.text();
    append(`console:${msg.type()}:${text}`);
    if (text.indexOf(SUITE_FINISHED_MARKER) >= 0) {
      suiteFinished = true;
    }
  });
  page.on('pageerror', err => append(`pageerror:${String(err)}`));
  page.on('requestfailed', req => append(`requestfailed:${req.url()} ${req.failure()?.errorText || ''}`));
  page.on('response', resp => {
    if (resp.status() >= 400) {
      append(`response:${resp.status()}:${resp.url()}`);
    }
  });

  // Opt-in worker CPU profiler (CN1_JS_PROFILE_WORKER=1). A synchronous infinite
  // loop in a translated method stops the worker's event loop but is still
  // sampled by V8's out-of-process profiler, so it dominates the profile -> the
  // hottest self-time node names the wedging method. Writes a .cpuprofile next
  // to LOG_FILE on close.
  if (profileWorker) {
    try {
      profiler = await startWorkerProfiler();
      // Wait for the VM worker to attach + start sampling, in the background.
      profiler.begin().catch(e => append(`profiler:beginError:${String(e)}`));
    } catch (e) {
      append(`profiler:startError:${String(e)}`);
      profiler = null;
    }
  }

  // Flush + analyze the CPU profile. Idempotent (guarded by profileFinalized)
  // so it runs exactly once whether via clean lifetime exit OR a SIGTERM from
  // the run-javascript-browser-tests.sh kill-timer (which fires before our
  // lifetime loop ends when the worker wedges).
  finalizeProfile = async () => {
    if (!profiler || profileFinalized) return;
    profileFinalized = true;
    try {
      const profile = await profiler.stop();
      if (profile) {
        // Aggregate self-time (hitCount) per function; the synchronous wedge
        // loop dominates, so the hottest self-time node names the method.
        const byFn = new Map();
        for (const node of profile.nodes || []) {
          const cf = node.callFrame || {};
          const key = `${cf.functionName || '(anon)'}  ${cf.url || ''}:${cf.lineNumber}`;
          byFn.set(key, (byFn.get(key) || 0) + (node.hitCount || 0));
        }
        const top = [...byFn.entries()].sort((a, b) => b[1] - a[1]).slice(0, 20);
        const total = [...byFn.values()].reduce((s, h) => s + h, 0) || 1;
        append('profiler:TOP_SELFTIME');
        for (const [key, hits] of top) {
          append(`profiler:hot:${(100 * hits / total).toFixed(1)}%  hits=${hits}  ${key}`);
        }
        if (logFile) {
          fs.writeFileSync(logFile.replace(/\.[^.]*$/, '') + '.cpuprofile', JSON.stringify(profile));
        }
      } else {
        append('profiler:stop:no-worker-session');
      }
    } catch (e) {
      append(`profiler:stopError:${String(e)}`);
    } finally {
      profiler.close();
    }
  };
  for (const sig of ['SIGTERM', 'SIGINT']) {
    process.on(sig, async () => {
      append(`profiler:signal:${sig}`);
      await finalizeProfile();
      try { await browser.close(); } catch { /* ignore */ }
      process.exit(0);
    });
  }

  append(`goto:${url}`);
  await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 60000 });
  await page.waitForTimeout(2000);

  const start = Date.now();
  while (Date.now() - start < timeoutSeconds * 1000) {
    const state = await page.evaluate(() => ({
      initialized: !!window.cn1Initialized,
      started: !!window.cn1Started,
      error: window.__parparError ? JSON.stringify(window.__parparError) : ''
    }));
    append(`state:${JSON.stringify(state)}`);
    if (state.error || suiteFinished) {
      break;
    }
    await page.waitForTimeout(1000);
  }
  await finalizeProfile();
} finally {
  await browser.close();
}
