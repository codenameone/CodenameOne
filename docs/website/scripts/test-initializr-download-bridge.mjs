import assert from "node:assert/strict";
import fs from "node:fs";
import vm from "node:vm";

const sourcePath = process.argv[2]
  || new URL("../../../scripts/initializr/javascript/src/main/javascript/com_codename1_initializr_WebsiteThemeNative.js", import.meta.url);
const source = fs.readFileSync(sourcePath, "utf8");

function loadBridge(click) {
  const nativeInterfaces = {};
  const posts = [];
  const actions = [];
  const parent = {
    postMessage(message) {
      actions.push("post");
      posts.push(message);
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
    }
  };

  vm.runInNewContext(source, {
    window,
    cn1_get_native_interfaces: () => nativeInterfaces
  });

  return {
    bridge: nativeInterfaces.com_codename1_initializr_WebsiteThemeNative,
    posts,
    actions
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

function invokeSteps(state, email) {
  let result;
  state.bridge.requestSteps__java_lang_String_java_lang_String_java_lang_String_java_lang_String(
    email,
    "com.Example.MyApp",
    "kotlin",
    "vs_code",
    { complete(value) { result = value; } }
  );
  return result;
}

{
  const state = loadBridge(() => {});
  assert.equal(invokeSteps(state, "dev@example.org"), true);
  assert.deepEqual(
    JSON.parse(JSON.stringify(state.posts)),
    [{
      type: "cn1-initializr-steps-request",
      email: "dev@example.org",
      packageName: "com.Example.MyApp",
      template: "kotlin",
      ide: "vs_code"
    }]
  );
  assert.deepEqual(state.actions, ["post"], "a steps request downloads nothing");
}

{
  const state = loadBridge(() => {});
  assert.equal(invokeSteps(state, ""), false, "no request without an email");
  assert.deepEqual(state.posts, []);
}

{
  // No embedding page (the app opened on its own): nothing to ask, so false.
  const nativeInterfaces = {};
  const window = { matchMedia() { return { matches: false }; } };
  window.parent = window;
  vm.runInNewContext(source, { window, cn1_get_native_interfaces: () => nativeInterfaces });
  let result;
  nativeInterfaces.com_codename1_initializr_WebsiteThemeNative
    .requestSteps__java_lang_String_java_lang_String_java_lang_String_java_lang_String(
      "dev@example.org", "a.b", "", "", { complete(value) { result = value; } });
  assert.equal(result, false);
}

console.log("Initializr download bridge tests passed");
