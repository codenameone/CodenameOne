import assert from "node:assert/strict";
import fs from "node:fs";
import vm from "node:vm";
import { fileURLToPath } from "node:url";

const scriptPath = fileURLToPath(new URL("../assets/js/cn1-exp-004.js", import.meta.url));
const source = fs.readFileSync(scriptPath, "utf8");

function storage(initial = {}) {
  const values = new Map(Object.entries(initial));
  return {
    getItem: (key) => values.has(key) ? values.get(key) : null,
    setItem: (key, value) => values.set(key, String(value)),
  };
}

function load({
  storedArm = null,
  randomValue = 0,
  hostname = "www.codenameone.com",
  search = "",
  consent = "accepted",
  legacyConsent = null,
} = {}) {
  const initial = storedArm ? { "cn1-exp-004-arm-v1": storedArm } : {};
  if (consent) initial["cn1-crisp-consent-v2"] = consent;
  if (legacyConsent) initial["cn1-crisp-consent-v1"] = legacyConsent;
  const localStorage = storage(initial);
  const documentElement = { dataset: {} };
  const window = {
    location: { hostname, search },
    crypto: {
      getRandomValues(values) {
        values[0] = randomValue;
        return values;
      },
    },
  };
  window.window = window;
  const context = vm.createContext({
    document: { documentElement, cookie: "" },
    localStorage,
    Math,
    Object,
    Uint32Array,
    URLSearchParams,
    window,
  });
  vm.runInContext(source, context, { filename: scriptPath });
  return { documentElement, localStorage, window };
}

{
  const ownership = load({ randomValue: 2 });
  assert.equal(ownership.window.cn1Exp004.id, "EXP-004");
  assert.equal(ownership.window.cn1Exp004.arm, "ownership");
  assert.equal(ownership.window.cn1Exp004.telemetryEnabled, true);
  assert.equal(ownership.documentElement.dataset.cn1Exp004Arm, "ownership");
  assert.equal(ownership.localStorage.getItem("cn1-exp-004-arm-v1"), "ownership");
}

{
  const reach = load({ randomValue: 3 });
  assert.equal(reach.window.cn1Exp004.arm, "reach");
  assert.equal(reach.documentElement.dataset.cn1Exp004Arm, "reach");
}

{
  const persisted = load({ storedArm: "reach", randomValue: 2 });
  assert.equal(persisted.window.cn1Exp004.arm, "reach",
    "a returning browser must keep its original arm");
}

{
  const invalid = load({ storedArm: "tampered", randomValue: 2 });
  assert.equal(invalid.window.cn1Exp004.arm, "ownership",
    "an invalid stored arm must be replaced with a valid assignment");
}

{
  const preview = load({
    storedArm: "ownership",
    hostname: "127.0.0.1",
    search: "?cn1_exp004=reach",
  });
  assert.equal(preview.window.cn1Exp004.arm, "reach");
  assert.equal(preview.window.cn1Exp004.telemetryEnabled, false,
    "local QA must not emit production experiment telemetry");
  assert.equal(preview.localStorage.getItem("cn1-exp-004-arm-v1"), "ownership",
    "a local visual preview must not rewrite the persisted assignment");
}

{
  const deployedPreview = load({
    storedArm: "reach",
    hostname: "exp-004-homepage-positioning.codenameone.pages.dev",
  });
  assert.equal(deployedPreview.window.cn1Exp004.arm, "ownership",
    "a deployed preview must show the control without experiment enrollment");
  assert.equal(deployedPreview.window.cn1Exp004.telemetryEnabled, false);
  assert.equal(deployedPreview.documentElement.dataset.cn1Exp004Arm, "ownership");
  assert.equal(deployedPreview.localStorage.getItem("cn1-exp-004-arm-v1"), "reach",
    "a deployed preview must not rewrite stored experiment state");
}

{
  const pendingConsent = load({ randomValue: 3, consent: null });
  assert.equal(pendingConsent.window.cn1Exp004.arm, "reach");
  assert.equal(pendingConsent.localStorage.getItem("cn1-exp-004-arm-v1"), null,
    "assignment storage must wait for consent");
  pendingConsent.localStorage.setItem("cn1-crisp-consent-v2", "accepted");
  assert.equal(pendingConsent.window.cn1Exp004.persist(), true);
  assert.equal(pendingConsent.localStorage.getItem("cn1-exp-004-arm-v1"), "reach",
    "the assigned arm should persist when consent is accepted");
}

{
  const legacyConsent = load({ randomValue: 3, consent: null, legacyConsent: "accepted" });
  assert.equal(legacyConsent.window.cn1Exp004.arm, "reach");
  assert.equal(legacyConsent.localStorage.getItem("cn1-exp-004-arm-v1"), null,
    "old chat-only consent must not authorize experiment persistence");
  assert.equal(legacyConsent.window.cn1Exp004.persist(), false);
}

console.log("EXP-004 assignment tests passed");
