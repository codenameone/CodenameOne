import assert from "node:assert/strict";
import fs from "node:fs";
import vm from "node:vm";
import { fileURLToPath } from "node:url";

const scriptPath = fileURLToPath(new URL("../assets/js/cn1-crisp.js", import.meta.url));
const source = fs.readFileSync(scriptPath, "utf8");

function storage() {
  const values = new Map();
  return {
    getItem: (key) => values.has(key) ? values.get(key) : null,
    setItem: (key, value) => values.set(key, String(value)),
    removeItem: (key) => values.delete(key),
  };
}

function control() {
  const listeners = {};
  return {
    style: {},
    addEventListener(type, listener) {
      listeners[type] = listener;
    },
    click() {
      listeners.click?.({ preventDefault() {} });
    },
    setAttribute() {},
    removeAttribute() {},
  };
}

function load(
  consent,
  search = "",
  pathname = "/playground/",
  experimentArm = null,
  experimentTelemetryEnabled = true,
  hostname = "www.codenameone.com",
  legacyConsent = null,
  sessionStore = storage(),
  fetchHandler = null
) {
  const timers = [];
  const fetches = [];
  let uuid = 0;
  const documentListeners = {};
  const banner = control();
  const accept = control();
  const decline = control();
  const cookies = new Map();
  if (consent) {
    cookies.set("cn1_crisp_consent_v2", consent);
  }
  if (legacyConsent) {
    cookies.set("cn1_crisp_consent", legacyConsent);
  }

  const document = {
    referrer: "",
    head: { appendChild() {} },
    querySelector(selector) {
      if (selector === "[data-cn1-cookie-banner]") return banner;
      if (selector === "[data-cn1-cookie-accept]") return accept;
      if (selector === "[data-cn1-cookie-decline]") return decline;
      return null;
    },
    querySelectorAll() { return []; },
    getElementById() { return null; },
    createElement() { return {}; },
    addEventListener(type, listener) {
      documentListeners[type] = listener;
    },
  };
  Object.defineProperty(document, "cookie", {
    get() {
      return Array.from(cookies, ([key, value]) => `${key}=${value}`).join("; ");
    },
    set(value) {
      const pair = value.split(";", 1)[0];
      const separator = pair.indexOf("=");
      cookies.set(pair.substring(0, separator), pair.substring(separator + 1));
    },
  });

  const localStore = storage();
  if (experimentArm) {
    localStore.setItem("cn1-exp-004-arm-v1", experimentArm);
  }

  const window = {
    crypto: {
      randomUUID() {
        uuid += 1;
        return `00000000-0000-4000-8000-${String(uuid).padStart(12, "0")}`;
      },
    },
    fetch(url, options) {
      fetches.push({ url, options });
      if (fetchHandler) {
        return Promise.resolve(fetchHandler(url, options, fetches));
      }
      if (url === "/api/exp004/session") {
        return Promise.resolve({
          ok: true,
          status: 201,
          json: async () => ({
            submission_token: "00000000-0000-4000-8000-999999999999",
          }),
        });
      }
      return Promise.resolve({ ok: true, status: 202 });
    },
    location: {
      hostname,
      pathname,
      search,
    },
    setTimeout(callback) {
      timers.push(callback);
      return timers.length;
    },
  };
  if (experimentArm) {
    window.cn1Exp004 = {
      id: "EXP-004",
      arm: experimentArm,
      telemetryEnabled: experimentTelemetryEnabled,
      persist() { return true; },
    };
  }
  window.window = window;

  const context = vm.createContext({
    Date,
    decodeURIComponent,
    document,
    encodeURIComponent,
    localStorage: localStore,
    sessionStorage: sessionStore,
    URL,
    URLSearchParams,
    window,
  });
  vm.runInContext(source, context, { filename: scriptPath });
  return {
    accept,
    decline,
    documentListeners,
    fetches,
    timers,
    window,
    sessionStorage: context.sessionStorage,
  };
}

async function settle(state, runTimers = false) {
  for (let round = 0; round < 8; round += 1) {
    await Promise.resolve();
  }
  if (runTimers) {
    while (state.timers.length) {
      const callbacks = state.timers.splice(0);
      callbacks.forEach((callback) => callback());
      for (let round = 0; round < 8; round += 1) {
        await Promise.resolve();
      }
    }
  }
}

function eventCommands(state) {
  return (state.window.$crisp || []).filter((command) => command[0] === "set");
}

function events(state) {
  return eventCommands(state).map((command) => command[2][0][0]);
}

{
  const state = load("accepted");
  state.window.cn1CrispEvents.gettingStartedDwell({ page: "/getting-started/" });
  state.decline.click();
  state.timers.forEach((callback) => callback());
  state.window.cn1CrispEvents.buildError({ message: "failed" });

  assert.equal(eventCommands(state).length, 0, "events must stop after consent is withdrawn");
  assert.equal(state.sessionStorage.getItem("cn1-crisp-ev-GettingStartedDwell"), null,
    "a blocked dwell event must not consume its session guard");
  assert.equal(state.sessionStorage.getItem("cn1-crisp-ev-BuildError"), null,
    "a blocked on-demand event must not consume its session guard");
}

{
  const state = load("accepted");
  state.window.cn1CrispEvents.signingScreenView({ page: "/signing/" });
  state.window.cn1CrispEvents.signingScreenView({ page: "/signing/" });

  assert.equal(eventCommands(state).length, 1, "an accepted event should be queued once");
  assert.equal(state.sessionStorage.getItem("cn1-crisp-ev-SigningScreenView"), "1");
}

{
  const state = load("accepted");
  state.window.cn1CrispEvents.initializrProjectDownloaded({ page: "/initializr/" });

  const recorded = events(state);
  assert.equal(recorded.length, 1, "an accepted project download should be queued once");
  assert.equal(recorded[0][0], "InitializrProjectDownloaded");
  assert.deepEqual(JSON.parse(JSON.stringify(recorded[0][1])), { page: "/initializr/" });
}

{
  const state = load("declined");
  state.window.cn1CrispEvents.initializrProjectDownloaded({ page: "/initializr/" });
  assert.equal(eventCommands(state).length, 0,
    "a project download must not be recorded without analytics consent");
}

{
  const state = load(null, "", "/", "ownership", true, "www.codenameone.com", "accepted");
  assert.equal(eventCommands(state).length, 0,
    "old chat-only consent must not authorize experiment telemetry");
  assert.equal(state.window.CRISP_WEBSITE_ID, undefined,
    "old chat-only consent must not load Crisp before the new opt-in");
}

{
  const state = load("accepted", "", "/", "ownership");
  state.window.cn1CrispEvents.initializrProjectDownloaded({ page: "/initializr/" });
  state.window.cn1CrispEvents.initializrProjectDownloaded({ page: "/initializr/" });
  await settle(state);

  const recorded = events(state);
  assert.equal(recorded.length, 3,
    "one homepage exposure may produce at most one arm-specific download");
  assert.equal(recorded[0][0], "Exp004OwnershipExposure");
  assert.equal(recorded[0][1].experiment_id, "EXP-004");
  assert.equal(recorded[0][1].experiment_arm, "ownership");
  assert.equal(
    state.sessionStorage.getItem(
      "cn1-crisp-ev-Exp004OwnershipExposure-exp-004-ownership"
    ),
    "1"
  );
  assert.equal(recorded[1][0], "InitializrProjectDownloaded");
  assert.equal(recorded[1][1].experiment_arm, "ownership");
  assert.equal(recorded[2][0], "Exp004OwnershipDownload");
  assert.equal(state.fetches.length, 3,
    "one rate-limited session plus two events must reach the counter");
  assert.deepEqual(
    state.fetches.map(({ url, options }) => ({
      url,
      method: options.method,
      keepalive: options.keepalive,
      body: JSON.parse(options.body),
    })),
    [
      {
        url: "/api/exp004/session",
        method: "POST",
        keepalive: true,
        body: {
          session_key: "00000000-0000-4000-8000-000000000001",
          arm: "ownership",
        },
      },
      {
        url: "/api/exp004/collect",
        method: "POST",
        keepalive: true,
        body: {
          event: "Exp004OwnershipExposure",
          event_id: "00000000-0000-4000-8000-000000000002",
          session_key: "00000000-0000-4000-8000-000000000001",
          submission_token: "00000000-0000-4000-8000-999999999999",
        },
      },
      {
        url: "/api/exp004/collect",
        method: "POST",
        keepalive: true,
        body: {
          event: "Exp004OwnershipDownload",
          event_id: "00000000-0000-4000-8000-000000000003",
          session_key: "00000000-0000-4000-8000-000000000001",
          submission_token: "00000000-0000-4000-8000-999999999999",
        },
      },
    ],
  );
}

{
  const state = load("accepted", "", "/playground/", "reach");
  state.window.cn1CrispEvents.conversionClick({ action: "playground-download" });

  const recorded = events(state);
  assert.equal(recorded.length, 1);
  assert.equal(recorded[0][0], "ConversionClick");
  assert.equal(recorded[0][1].experiment_arm, undefined,
    "a conversion without a current homepage exposure must not inherit a stored arm");
}

{
  const state = load(null, "", "/", "reach");
  assert.equal(eventCommands(state).length, 0, "experiment exposure must wait for consent");
  state.accept.click();
  await settle(state);
  const recorded = events(state);
  assert.equal(recorded.length, 1);
  assert.equal(recorded[0][0], "Exp004ReachExposure");
  assert.equal(state.fetches.length, 2,
    "accepting consent must authorize a session and record the exposure denominator");
}

{
  const state = load("declined", "", "/", "ownership");
  assert.equal(eventCommands(state).length, 0,
    "a declined homepage view must not be recorded");
  state.accept.click();
  const recorded = events(state);
  assert.equal(recorded.length, 1,
    "opting in after a prior decline must record the exposure denominator");
  assert.equal(recorded[0][0], "Exp004OwnershipExposure");
}

{
  const state = load("accepted", "", "/", "reach", false);
  assert.equal(eventCommands(state).length, 0,
    "a preview assignment must never emit production experiment telemetry");
  assert.equal(state.fetches.length, 0,
    "a preview assignment must never hit the first-party counter");
}

{
  const state = load(
    "accepted",
    "",
    "/",
    "reach",
    true,
    "exp-004-homepage-positioning.codenameone.pages.dev"
  );
  assert.equal(eventCommands(state).length, 0,
    "a Cloudflare preview host must never emit production experiment telemetry");
  assert.equal(state.fetches.length, 0,
    "a Cloudflare preview must never hit the first-party counter");
}

{
  const state = load("accepted", "", "/initializr/", "reach");
  state.window.cn1CrispEvents.initializrProjectDownloaded({ page: "/initializr/" });

  const recorded = events(state);
  assert.equal(recorded.length, 1,
    "a direct project download must not be attributed without a current homepage exposure");
  assert.equal(recorded[0][0], "InitializrProjectDownloaded");
  assert.deepEqual(JSON.parse(JSON.stringify(recorded[0][1])), { page: "/initializr/" });
  assert.equal(state.fetches.length, 0,
    "an unexposed download must not hit the experiment counter");
}

{
  const sharedSession = storage();
  const first = load(
    "accepted", "", "/", "ownership", true, "www.codenameone.com", null,
    sharedSession,
    (url) => url === "/api/exp004/session"
      ? {
          ok: true,
          status: 201,
          json: async () => ({
            submission_token: "00000000-0000-4000-8000-999999999999",
          }),
        }
      : { ok: false, status: 503 }
  );
  await settle(first);
  const failedBody = JSON.parse(
    first.fetches.find(({ url }) => url === "/api/exp004/collect").options.body
  );

  const recovered = load(
    "accepted", "", "/", "ownership", true, "www.codenameone.com", null,
    sharedSession
  );
  await settle(recovered);
  const recoveredBody = JSON.parse(
    recovered.fetches.find(({ url }) => url === "/api/exp004/collect").options.body
  );
  assert.equal(recoveredBody.event_id, failedBody.event_id,
    "a reload must retry a failed counter post with its persisted event ID");
  assert.equal(
    sharedSession.getItem("cn1-exp-004-counter-ack-v1-Exp004OwnershipExposure"),
    "1",
    "only a successful response may acknowledge the persisted event"
  );
}

{
  const unavailableStorage = {
    getItem() { throw new Error("storage unavailable"); },
    setItem() { throw new Error("storage unavailable"); },
    removeItem() { throw new Error("storage unavailable"); },
  };
  const state = load(
    "accepted", "", "/", "reach", true, "www.codenameone.com", null,
    unavailableStorage
  );
  await settle(state);
  assert.equal(state.fetches.filter(({ url }) => url === "/api/exp004/collect").length, 1,
    "storage-disabled visitors must still get best-effort in-memory telemetry");
}

{
  const state = load(null);
  state.window.cn1CrispEvents.signingScreenView({ page: "/signing/" });
  assert.equal(eventCommands(state).length, 0, "an event must wait for a consent choice");
  state.accept.click();
  assert.equal(eventCommands(state).length, 1, "an explicitly accepted pending page event should be queued");
}

{
  const state = load(
    "accepted",
    "?utm_source=github&utm_medium=oss&utm_campaign=repo-readme&utm_content=playground"
  );
  state.window.cn1CrispEvents.conversionClick({ action: "playground-download" });

  const recorded = events(state);
  assert.equal(recorded[0][0], "OssArrival", "an accepted OSS landing should be recorded once");
  assert.deepEqual(
    JSON.parse(JSON.stringify(recorded[0][1])),
    {
      source: "github",
      campaign: "repo-readme",
      content: "playground",
      page: "/playground/",
    }
  );
  assert.equal(recorded[1][0], "ConversionClick");
  assert.equal(recorded[1][1].oss_source, "github");
  assert.equal(recorded[1][1].oss_campaign, "repo-readme");
  assert.equal(recorded[1][1].oss_content, "playground");
}

{
  const state = load(
    null,
    "?utm_source=github&utm_medium=oss&utm_campaign=repo-readme"
  );
  assert.equal(eventCommands(state).length, 0, "OSS attribution must wait for consent");
  assert.equal(state.sessionStorage.getItem("cn1-oss-attribution-v1"), null,
    "OSS attribution must not be stored before consent");
}

console.log("cn1-crisp event consent tests passed");
