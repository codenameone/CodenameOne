// The Initializr nudges its "Generate Project" button left of the host page's
// Crisp widget. The clearance is measured from what Crisp actually shows over
// the bottom of the frame: a first-time visitor sees Crisp folded with a wide
// greeting pill, which a fixed 64px-bubble allowance left covering the button.

import assert from "node:assert/strict";
import fs from "node:fs";
import vm from "node:vm";

const sourcePath = process.argv[2]
  || new URL("../../../scripts/initializr/javascript/src/main/javascript/com_codename1_initializr_WebsiteThemeNative.js", import.meta.url);
const source = fs.readFileSync(sourcePath, "utf8");

// Frame fills a 1400x1100 page below a 73px header, like the live page.
const FRAME = { left: 0, top: 73, right: 1400, bottom: 1100 };

function el(rect, style = {}) {
  return {
    rect: { width: rect.right - rect.left, height: rect.bottom - rect.top, ...rect },
    style: { display: "block", visibility: "visible", opacity: "1", ...style },
    getBoundingClientRect() { return this.rect; }
  };
}

function clearance(crispNodes, clientStyle) {
  const client = crispNodes === null ? null : {
    ...el({ left: 0, top: 0, right: 0, bottom: 0 }, clientStyle),
    querySelectorAll: () => crispNodes
  };
  const parent = {
    innerWidth: 1400,
    innerHeight: 1100,
    document: { querySelector: (q) => (q === ".crisp-client" ? client : null) },
    getComputedStyle: (node) => node.style
  };
  const window = {
    parent,
    frameElement: { getBoundingClientRect: () => FRAME },
    document: {},
    matchMedia: () => ({ matches: false })
  };
  const nativeInterfaces = {};
  vm.runInNewContext(source, { window, cn1_get_native_interfaces: () => nativeInterfaces });
  let value;
  nativeInterfaces.com_codename1_initializr_WebsiteThemeNative
    .chatLauncherClearance_({ complete(v) { value = v; } });
  return value;
}

// No Crisp (consent declined or not loaded yet): nothing reserved.
assert.equal(clearance(null), 0);
// Crisp present but hidden.
assert.equal(clearance([el({ left: 1312, top: 1012, right: 1376, bottom: 1076 })], { display: "none" }), 0);

// Round launcher only: 64px bubble 24px from the edge -> 88 + 16 gap.
assert.equal(clearance([el({ left: 1312, top: 1012, right: 1376, bottom: 1076 })]), 104);

// Folded with a greeting pill beside the bubble: clear the pill's left edge.
assert.equal(clearance([
  el({ left: 1312, top: 1012, right: 1376, bottom: 1076 }),
  el({ left: 1010, top: 1020, right: 1300, bottom: 1068 })
]), 406);

// Invisible Crisp scaffolding (opacity 0, zero-size) is ignored.
assert.equal(clearance([
  el({ left: 1312, top: 1012, right: 1376, bottom: 1076 }),
  el({ left: 400, top: 1000, right: 1300, bottom: 1080 }, { opacity: "0" }),
  el({ left: 200, top: 1050, right: 200, bottom: 1050 })
]), 104);

// A greeting card that floats above the action bar does not move the button.
assert.equal(clearance([
  el({ left: 1312, top: 1012, right: 1376, bottom: 1076 }),
  el({ left: 1000, top: 700, right: 1376, bottom: 900 })
]), 104);

// The open chat box can span most of the page: cap at 60% of the frame width.
assert.equal(clearance([el({ left: 0, top: 300, right: 1376, bottom: 1076 })]), 840);

console.log("Initializr chat clearance tests passed");
