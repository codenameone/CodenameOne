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

function load(consent, search = "") {
  const timers = [];
  const documentListeners = {};
  const banner = control();
  const accept = control();
  const decline = control();
  const cookies = new Map();
  if (consent) {
    cookies.set("cn1_crisp_consent", consent);
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

  const window = {
    location: {
      pathname: "/playground/",
      search,
    },
    setTimeout(callback) {
      timers.push(callback);
      return timers.length;
    },
  };
  window.window = window;

  const context = vm.createContext({
    Date,
    decodeURIComponent,
    document,
    encodeURIComponent,
    localStorage: storage(),
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
