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
  legacyConsent = null
) {
  const timers = [];
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
    sessionStorage: storage(),
    URL,
    URLSearchParams,
    window,
  });
  vm.runInContext(source, context, { filename: scriptPath });
  return { accept, decline, documentListeners, timers, window, sessionStorage: context.sessionStorage };
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
  const recorded = events(state);
  assert.equal(recorded.length, 1, "an eligible homepage view should record one arm exposure");
  assert.equal(recorded[0][0], "Exp004OwnershipExposure");
  assert.equal(recorded[0][1].experiment_id, "EXP-004");
  assert.equal(recorded[0][1].experiment_arm, "ownership");
}

{
  const state = load(null, "", "/", "reach");
  assert.equal(eventCommands(state).length, 0, "experiment exposure must wait for consent");
  state.accept.click();
  const recorded = events(state);
  assert.equal(recorded.length, 1);
  assert.equal(recorded[0][0], "Exp004ReachExposure");
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
}

{
  const state = load("accepted", "", "/initializr/", "reach");
  state.window.cn1CrispEvents.initializrProjectDownloaded({ page: "/initializr/" });

  const recorded = events(state);
  assert.equal(recorded.length, 2, "a project download should keep the base event and add its arm event");
  assert.equal(recorded[0][0], "InitializrProjectDownloaded");
  assert.equal(recorded[0][1].experiment_arm, "reach");
  assert.equal(recorded[1][0], "Exp004ReachDownload");
  assert.equal(recorded[1][1].experiment_id, "EXP-004");
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
