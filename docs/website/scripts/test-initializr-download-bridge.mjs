import assert from "node:assert/strict";
import fs from "node:fs";
import vm from "node:vm";

const sourcePath = process.argv[2]
  || new URL("../../../scripts/initializr/javascript/src/main/javascript/com_codename1_initializr_WebsiteThemeNative.js", import.meta.url);
const source = fs.readFileSync(sourcePath, "utf8");

const SITE = "https://www.codenameone.com";

// parentOrigin: what window.parent.location.origin reads as; null makes reading
// it throw, which is what a browser does for a cross-origin parent.
function loadBridge(click, parentOrigin = SITE) {
  const nativeInterfaces = {};
  const posts = [];
  const targets = [];
  const actions = [];
  const parent = {
    postMessage(message, targetOrigin) {
      actions.push("post");
      posts.push(message);
      targets.push(targetOrigin);
    },
    get location() {
      if (parentOrigin === null) throw new Error("SecurityError: cross-origin frame");
      return { origin: parentOrigin };
    }
  };
  const body = {
    appendChild(anchor) {
      actions.push("append");
      anchor.parentNode = body;
    },
    removeChild(anchor) {
      actions.push("remove");
      anchor.parentNode = null;
    }
  };
  const window = {
    parent,
    document: {
      body,
      createElement() {
        return {
          click() {
            actions.push("click");
            click();
          }
        };
      }
    },
    matchMedia() {
      return { matches: false };
    },
    // Message listeners and timers the steps round trip uses, driven by hand.
    addEventListener(type, fn) {
      if (type === "message") listeners.add(fn);
    },
    removeEventListener(type, fn) {
      if (type === "message") listeners.delete(fn);
    },
    setTimeout(fn) {
      timers.push(fn);
    }
  };
  const listeners = new Set();
  const timers = [];

  vm.runInNewContext(source, {
    window,
    cn1_get_native_interfaces: () => nativeInterfaces
  });

  return {
    bridge: nativeInterfaces.com_codename1_initializr_WebsiteThemeNative,
    posts,
    targets,
    actions,
    // The host page answering a request (evt.source is the parent window).
    answer(data, source = parent, origin = SITE) {
      for (const fn of [...listeners]) fn({ source, data, origin });
    },
    expire() {
      for (const fn of timers.splice(0)) fn();
    },
    listenerCount: () => listeners.size
  };
}

function invokeDownload(state) {
  let result;
  state.bridge.downloadProject__java_lang_String_java_lang_String_java_lang_String_java_lang_String(
    "sample.zip",
    "data:application/octet-stream;base64,AA==",
    "com.Example.MyApp",
    "barebones",
    { complete(value) { result = value; } }
  );
  return result;
}

{
  const state = loadBridge(() => {});
  assert.equal(invokeDownload(state), true);
  assert.deepEqual(state.actions, ["append", "click", "remove", "post"]);
  assert.deepEqual(
    JSON.parse(JSON.stringify(state.posts)),
    [{
      type: "cn1-initializr-project-downloaded",
      packageName: "com.Example.MyApp",
      template: "barebones"
    }]
  );
}

{
  const state = loadBridge(() => { throw new Error("blocked"); });
  assert.equal(invokeDownload(state), false);
  assert.deepEqual(state.actions, ["append", "click", "remove"]);
  assert.deepEqual(state.posts, []);
}

const STEPS = "requestSteps__java_lang_String_java_lang_String_java_lang_String_java_lang_String_java_lang_String";

function invokeSteps(state, email) {
  const result = { value: undefined };
  state.bridge[STEPS](email, "com.Example.MyApp", "kotlin", "vscode", "gradle",
    { complete(value) { result.value = value; } });
  return result;
}

function canRequest(state) {
  let value;
  state.bridge.canRequestSteps_({ complete(v) { value = v; } });
  return value;
}

{
  // Embedded in the Codename One website: the request goes to that origin only,
  // carries an id, and only the host's matching answer completes it.
  const state = loadBridge(() => {});
  assert.equal(canRequest(state), true);
  const result = invokeSteps(state, "dev@example.org");
  assert.equal(result.value, undefined, "no answer yet: the panel must wait, not claim success");
  assert.equal(state.posts.length, 1);
  assert.deepEqual(state.targets, [SITE], "never posted to *: only the trusted parent origin");
  const sent = JSON.parse(JSON.stringify(state.posts[0]));
  assert.ok(/^steps-\d+-\d+$/.test(sent.id), "request id: " + sent.id);
  delete sent.id;
  assert.deepEqual(sent, {
    type: "cn1-initializr-steps-request",
    email: "dev@example.org",
    packageName: "com.Example.MyApp",
    template: "kotlin",
    ide: "vscode",
    build: "gradle"
  });
  assert.deepEqual(state.actions, ["post"], "a steps request downloads nothing");

  const id = state.posts[0].id;
  state.answer({ type: "cn1-initializr-steps-result", id: "someone-else", ok: true });
  state.answer({ type: "cn1-initializr-steps-result", id, ok: true }, {});
  state.answer({ type: "cn1-initializr-steps-result", id, ok: true }, undefined, "https://evil.example");
  assert.equal(result.value, undefined, "another id, another window or another origin is ignored");

  state.answer({ type: "cn1-initializr-steps-result", id, ok: true });
  assert.equal(result.value, true);
  assert.equal(state.listenerCount(), 0, "the listener is removed once answered");
  state.expire();
  assert.equal(result.value, true, "a late timeout does not overturn the answer");
}

for (const [label, origin] of [
  ["a third-party page framing /initializr-app/ (cross-origin: reading its location throws)", null],
  ["a same-origin page that is not the Codename One website (localhost, a preview)", "http://localhost:1313"],
]) {
  const state = loadBridge(() => {}, origin);
  assert.equal(canRequest(state), false, label + ": no email field");
  assert.equal(invokeSteps(state, "dev@example.org").value, false, label);
  assert.deepEqual(state.posts, [], label + ": the address is never posted");
}

{
  // The host could not send (no beacon, no WebCrypto, offline).
  const state = loadBridge(() => {});
  const result = invokeSteps(state, "dev@example.org");
  state.answer({ type: "cn1-initializr-steps-result", id: state.posts[0].id, ok: false });
  assert.equal(result.value, false);
}

{
  // No answer at all: the timeout reports not sent.
  const state = loadBridge(() => {});
  const result = invokeSteps(state, "dev@example.org");
  state.expire();
  assert.equal(result.value, false);
  assert.equal(state.listenerCount(), 0);
}

{
  const state = loadBridge(() => {});
  assert.equal(invokeSteps(state, "").value, false, "no request without an email");
  assert.deepEqual(state.posts, []);
}

{
  // No embedding page (the app opened on its own): nothing to ask, so false.
  const nativeInterfaces = {};
  const window = { matchMedia() { return { matches: false }; } };
  window.parent = window;
  vm.runInNewContext(source, { window, cn1_get_native_interfaces: () => nativeInterfaces });
  let result;
  nativeInterfaces.com_codename1_initializr_WebsiteThemeNative[STEPS](
    "dev@example.org", "a.b", "", "", "", { complete(value) { result = value; } });
  assert.equal(result, false);
}

console.log("Initializr download bridge tests passed");
