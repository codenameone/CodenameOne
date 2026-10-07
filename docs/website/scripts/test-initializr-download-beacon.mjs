// Tests the anonymous Initializr download beacon (assets/js/cn1-initializr-beacon.js)
// and the page wiring in layouts/_default/initializr.html that forwards the
// iframe's "cn1-initializr-project-downloaded" message to it.
import assert from "node:assert/strict";
import crypto from "node:crypto";
import fs from "node:fs";
import vm from "node:vm";
import { fileURLToPath } from "node:url";

const beaconSource = fs.readFileSync(
  fileURLToPath(new URL("../assets/js/cn1-initializr-beacon.js", import.meta.url)), "utf8");
const pageTemplate = fs.readFileSync(
  fileURLToPath(new URL("../layouts/_default/initializr.html", import.meta.url)), "utf8");

const ENDPOINT = "https://cloud.codenameone.com/api/v2/funnel/initializr-download";

function sha256Hex(text) {
  return crypto.createHash("sha256").update(text, "utf8").digest("hex");
}

function loadBeacon({ hostname = "www.codenameone.com", fetchImpl, subtle = crypto.webcrypto.subtle } = {}) {
  const calls = [];
  const timers = [];
  const window = {
    setTimeout(fn, ms) { timers.push({ fn, ms }); return timers.length; },
    clearTimeout(id) { if (timers[id - 1]) timers[id - 1].cleared = true; },
    location: { hostname },
    crypto: subtle ? { subtle } : undefined,
    fetch: fetchImpl === null ? undefined : (url, init) => {
      calls.push({ url, init });
      return fetchImpl ? fetchImpl(url, init) : Promise.resolve({ ok: true });
    },
  };
  vm.runInNewContext(beaconSource, {
    window, TextEncoder, Uint8Array, Promise, encodeURIComponent, String, AbortController,
  });
  return { beacon: window.cn1InitializrBeacon, calls, timers };
}

// Hash correctness: lower-cased package name, lowercase hex SHA-256, form body.
{
  const { beacon, calls } = loadBeacon();
  assert.equal(await beacon.send("com.Example.MyApp", "barebones"), true);
  assert.equal(calls.length, 1);
  const { url, init } = calls[0];
  assert.equal(url, ENDPOINT);
  assert.equal(init.method, "POST");
  assert.equal(init.credentials, "omit", "the beacon must not carry cookies");
  assert.equal(init.keepalive, true);
  assert.equal(init.referrerPolicy, "no-referrer");
  assert.equal(init.headers["Content-Type"], "application/x-www-form-urlencoded");
  const expected = sha256Hex("com.example.myapp");
  assert.match(expected, /^[0-9a-f]{64}$/);
  assert.equal(init.body, `pkg=${expected}&template=barebones`);
  const params = new URLSearchParams(init.body);
  assert.deepEqual([...params.keys()], ["pkg", "template"], "no other identifiers");
  assert.ok(!init.body.toLowerCase().includes("example"), "the package name never leaves in clear");
}

// Known vector, independent of node's hashing: SHA-256("com.example.myapp").
{
  const { beacon, calls } = loadBeacon({ hostname: "codenameone.com" });
  await beacon.send("COM.EXAMPLE.MYAPP", "");
  assert.equal(new URLSearchParams(calls[0].init.body).get("pkg"),
    "f110a249cb67c33e0d30f847bc485759068f15989484537ed28d97c7c63ecae7");
  assert.equal(new URLSearchParams(calls[0].init.body).get("template"), "");
}

// Failures are swallowed: a rejecting or throwing fetch never rejects send().
{
  const rejecting = loadBeacon({ fetchImpl: () => Promise.reject(new TypeError("network")) });
  assert.equal(await rejecting.beacon.send("com.example.app", "kotlin"), false,
    "a request that never left reports false, and still does not reject");
  assert.equal(rejecting.calls.length, 1);

  const throwing = loadBeacon({ fetchImpl: () => { throw new Error("sync failure"); } });
  assert.equal(await throwing.beacon.send("com.example.app", "kotlin"), false);

  const brokenCrypto = loadBeacon({ subtle: { digest: () => Promise.reject(new Error("no crypto")) } });
  assert.equal(await brokenCrypto.beacon.send("com.example.app", "kotlin"), false);
  assert.equal(brokenCrypto.calls.length, 0);
}

// No beacon off production, without a package name, or without crypto/fetch.
for (const [label, options, pkg] of [
  ["localhost", { hostname: "localhost" }, "com.example.app"],
  ["preview host", { hostname: "pr-1.codenameone.pages.dev" }, "com.example.app"],
  ["empty package", {}, ""],
  ["no subtle crypto", { subtle: null }, "com.example.app"],
]) {
  const { beacon, calls } = loadBeacon(options);
  assert.equal(await beacon.send(pkg, "barebones"), false, label);
  assert.equal(calls.length, 0, label);
}
{
  const { beacon } = loadBeacon({ fetchImpl: null });
  assert.equal(await beacon.send("com.example.app", "barebones"), false);
}

// sendSteps: the steps endpoint, the visitor's email, the hashed package, the
// template, IDE and build kind, and nothing else -- under the same transport rules.
const STEPS_ENDPOINT = "https://cloud.codenameone.com/api/v2/funnel/initializr-steps";
{
  const { beacon, calls } = loadBeacon();
  assert.equal(await beacon.sendSteps(" dev@example.org ", "COM.Example.MyApp", "kotlin", "intellij", "gradle"), true);
  assert.equal(calls.length, 1);
  const { url, init } = calls[0];
  assert.equal(url, STEPS_ENDPOINT);
  assert.equal(init.method, "POST");
  assert.equal(init.mode, "cors", "the steps request must read the server's answer");
  assert.equal(init.credentials, "omit", "the steps request must not carry cookies");
  assert.equal(init.keepalive, true);
  assert.equal(init.referrerPolicy, "no-referrer");
  assert.equal(init.headers["Content-Type"], "application/x-www-form-urlencoded");
  const params = new URLSearchParams(init.body);
  assert.deepEqual([...params.keys()], ["pkg", "email", "template", "ide", "build"], "exactly these fields");
  assert.equal(params.get("build"), "gradle");
  assert.equal(params.get("pkg"), "f110a249cb67c33e0d30f847bc485759068f15989484537ed28d97c7c63ecae7");
  assert.equal(params.get("email"), "dev@example.org");
  assert.equal(params.get("template"), "kotlin");
  assert.equal(params.get("ide"), "intellij");
  assert.ok(!init.body.toLowerCase().includes("myapp"), "the package name never leaves in clear");
}
// No steps request without an email, without a package, or off production.
for (const [label, options, email, pkg] of [
  ["empty email", {}, "", "com.example.app"],
  ["blank email", {}, "   ", "com.example.app"],
  ["missing email", {}, undefined, "com.example.app"],
  ["empty package", {}, "dev@example.org", ""],
  ["localhost", { hostname: "localhost" }, "dev@example.org", "com.example.app"],
  ["preview host", { hostname: "pr-1.codenameone.pages.dev" }, "dev@example.org", "com.example.app"],
  ["no subtle crypto", { subtle: null }, "dev@example.org", "com.example.app"],
]) {
  const { beacon, calls } = loadBeacon(options);
  assert.equal(await beacon.sendSteps(email, pkg, "barebones", "vs_code"), false, label);
  assert.equal(calls.length, 0, label);
}
{
  const rejecting = loadBeacon({ fetchImpl: () => Promise.reject(new TypeError("network")) });
  assert.equal(await rejecting.beacon.sendSteps("dev@example.org", "com.example.app", "", ""), false,
    "an email request that never left must not be reported as sent");
}

// The panel says "Check your inbox" only on a server-confirmed success: a
// refused (400), rate-limited (429) or failed (503) request resolves false,
// while the fire-and-forget download beacon is unaffected by the status.
for (const status of [400, 429, 503]) {
  const refused = loadBeacon({ fetchImpl: () => Promise.resolve({ ok: false, status }) });
  assert.equal(await refused.beacon.sendSteps("dev@example.org", "com.example.app", "", "", "maven"), false,
    "HTTP " + status + " is not success");
  assert.equal(await refused.beacon.send("com.example.app", ""), true, "the download beacon ignores status");
  assert.equal(refused.calls[1].init.mode, "no-cors", "the download beacon stays opaque");
}
{
  const accepted = loadBeacon({ fetchImpl: () => Promise.resolve({ ok: true, status: 202 }) });
  assert.equal(await accepted.beacon.sendSteps("dev@example.org", "com.example.app", "", "", "maven"), true);
}

// A steps request that hangs is aborted at the page's deadline and reported
// as not sent -- before the Initializr bridge's own (longer) wait runs out, so
// the panel never says "not sent" for a request still in flight.
{
  let finishDigest, requestStarted;
  const digest = new Promise(resolve => { finishDigest = resolve; });
  const started = new Promise(resolve => { requestStarted = resolve; });
  const hanging = loadBeacon({
    subtle: { digest: () => digest },
    fetchImpl: (url, init) => new Promise((resolve, reject) => {
      init.signal.addEventListener("abort", () => reject(new Error("AbortError")));
      requestStarted();
    }),
  });
  const pending = hanging.beacon.sendSteps("dev@example.org", "com.example.app", "", "", "maven");
  // WebCrypto completes independently of timers. Control that boundary instead
  // of assuming a zero-delay timer means the digest and request have started.
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(hanging.timers.length, 0, "hashing has not started a request deadline");
  finishDigest(new Uint8Array(32).buffer);
  await started;
  assert.equal(hanging.timers.length, 1, "a deadline is armed for the confirmed request");
  assert.ok(hanging.timers[0].ms < 20000, "shorter than the bridge's 20 s wait");
  hanging.timers[0].fn();
  assert.equal(await pending, false);
}
{
  const quick = loadBeacon({ fetchImpl: () => Promise.resolve({ ok: true, status: 202 }) });
  assert.equal(await quick.beacon.sendSteps("dev@example.org", "com.example.app", "", "", "maven"), true);
  assert.ok(quick.timers[0].cleared, "the deadline is cleared once the server answered");
  const beacon = loadBeacon();
  await beacon.beacon.send("com.example.app", "");
  assert.equal(beacon.timers.length, 0, "the fire-and-forget download beacon arms no deadline");
}

// Without AbortController a confirmed request could not be cancelled at its
// deadline, so it is not sent at all; the fire-and-forget beacon still is.
{
  const calls = [];
  const window = {
    location: { hostname: "www.codenameone.com" },
    crypto: { subtle: crypto.webcrypto.subtle },
    setTimeout() { return 1; }, clearTimeout() {},
    fetch: (url, init) => { calls.push(url); return Promise.resolve({ ok: true }); },
  };
  vm.runInNewContext(beaconSource, { window, TextEncoder, Uint8Array, Promise, encodeURIComponent, String });
  assert.equal(await window.cn1InitializrBeacon.sendSteps("dev@example.org", "a.b", "", "", "maven"), false);
  assert.equal(await window.cn1InitializrBeacon.send("a.b", ""), true);
  assert.deepEqual(calls, ["https://cloud.codenameone.com/api/v2/funnel/initializr-download"],
    "only the download beacon went out");
}

// Page wiring: one download message -> exactly one beacon + one Crisp event,
// and a throwing beacon does not stop the Crisp event.
function runPage({ beaconThrows = false, stepsResult = true, noBeacon = false } = {}) {
  const scripts = [...pageTemplate.matchAll(/<script>([\s\S]*?)<\/script>/g)];
  assert.ok(scripts.length > 0, "inline page script found");
  const pageScript = scripts[scripts.length - 1][1];
  const sends = [];
  const steps = [];
  const crisp = [];
  const listeners = {};
  const answers = [];
  const frameWindow = { postMessage(message, origin) { answers.push([message, origin]); } };
  const element = () => ({
    addEventListener() {},
    classList: { add() {}, toggle() {}, contains: () => false },
    style: { setProperty() {} },
    offsetHeight: 76,
  });
  const frame = { ...element(), contentWindow: frameWindow };
  const window = {
    addEventListener(type, listener) { listeners[type] = listener; },
    requestAnimationFrame(fn) { fn(); },
    setTimeout() {},
    matchMedia: () => ({ matches: false, addEventListener() {} }),
    cn1InitializrBeacon: {
      send(pkg, template) {
        sends.push([pkg, template]);
        if (beaconThrows) throw new Error("beacon failure");
        return Promise.resolve(true);
      },
      sendSteps(email, pkg, template, ide, build) {
        steps.push([email, pkg, template, ide, build]);
        if (beaconThrows) throw new Error("beacon failure");
        return Promise.resolve(stepsResult);
      },
    },
    cn1CrispEvents: { initializrProjectDownloaded: (data) => crisp.push(data) },
  };
  const document = {
    body: element(),
    documentElement: element(),
    getElementById: (id) => (id === "cn1-initializr-frame" ? frame : element()),
    querySelector: () => element(),
    addEventListener() {},
  };
  if (noBeacon) delete window.cn1InitializrBeacon;
  vm.runInNewContext(pageScript, {
    window, document, localStorage: { getItem: () => null },
  });
  const post = (data, source = frameWindow) =>
    listeners.message({ source, data, origin: "https://www.codenameone.com" });
  return { post, sends, steps, crisp, answers };
}

{
  const page = runPage();
  page.post({ type: "cn1-initializr-project-downloaded", packageName: "com.Example.App", template: "kotlin" });
  assert.deepEqual(page.sends, [["com.Example.App", "kotlin"]]);
  assert.equal(page.crisp.length, 1);
  page.post({ type: "cn1-initializr-ui-ready" });
  page.post({ type: "cn1-initializr-project-downloaded", packageName: "x.y" }, {});
  assert.equal(page.sends.length, 1, "other messages and other sources do not fire the beacon");
  page.post({ type: "cn1-initializr-project-downloaded", packageName: "com.Example.App", template: "kotlin" });
  assert.equal(page.sends.length, 2, "one beacon per download");
}
{
  const page = runPage({ beaconThrows: true });
  page.post({ type: "cn1-initializr-project-downloaded", packageName: "com.example.app", template: "" });
  assert.equal(page.sends.length, 1);
  assert.equal(page.crisp.length, 1, "a beacon failure must not block the Crisp event");
}

{
  const page = runPage();
  page.post({ type: "cn1-initializr-steps-request", email: "dev@example.org", build: "maven-backend",
    packageName: "com.Example.App", template: "barebones", ide: "eclipse" });
  assert.deepEqual(page.steps, [["dev@example.org", "com.Example.App", "barebones", "eclipse", "maven-backend"]]);
  assert.equal(page.sends.length, 0, "a steps request is not a download");
  assert.equal(page.crisp.length, 0, "a steps request is not a download");
  page.post({ type: "cn1-initializr-steps-request", email: "x@y.zz", packageName: "a.b" }, {});
  assert.equal(page.steps.length, 1, "other sources do not send steps");
}
{
  const page = runPage({ beaconThrows: true });
  page.post({ type: "cn1-initializr-steps-request", id: "s1", email: "dev@example.org", packageName: "a.b" });
  assert.equal(page.steps.length, 1, "a throwing beacon is swallowed");
  assert.deepEqual(JSON.parse(JSON.stringify(page.answers)),
    [[{ type: "cn1-initializr-steps-result", id: "s1", ok: false }, "https://www.codenameone.com"]],
    "a throwing beacon is answered as not sent");
}

// The page answers every steps request with the same id and whether the beacon
// really issued it, so the panel never says "Check your inbox" for nothing.
const tick = () => new Promise((resolve) => setTimeout(resolve, 0));
for (const [label, options, ok] of [
  ["sent", {}, true],
  ["beacon could not send (preview host, no fetch/WebCrypto, offline)", { stepsResult: false }, false],
  ["no beacon on the page", { noBeacon: true }, false],
]) {
  const page = runPage(options);
  page.post({ type: "cn1-initializr-steps-request", id: "steps-7", email: "dev@example.org", packageName: "a.b" });
  await tick();
  assert.deepEqual(JSON.parse(JSON.stringify(page.answers)),
    [[{ type: "cn1-initializr-steps-result", id: "steps-7", ok }, "https://www.codenameone.com"]], label);
}

console.log("Initializr download beacon tests passed");
