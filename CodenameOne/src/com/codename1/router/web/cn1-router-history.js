/*
 * cn1-router-history.js
 *
 * Browser-history bridge for the Codename One Router on the JavaScript port.
 * Pairs with com.codename1.router.web.JsRouterBootstrap.
 *
 * Usage: include this script in the HTML page that hosts the CN1 app, AFTER
 * the parparvm runtime. Ensure `window.cn1OutboxDispatch` (the CN1 outbox)
 * exists by the time the app calls JsRouterBootstrap.install() — the shim
 * tolerates either order.
 *
 *   <script src="parparvm_runtime.js"></script>
 *   <script src="cn1-router-history.js"></script>
 *
 * Protocol (matches JsRouterBootstrap.MESSAGE_CODE = 0x43524831):
 *
 *   App  -> Shim:  cn1inbox event { code: 0x43524831, detail: "push:/path" }
 *                                    or "replace:/path"
 *   Shim -> App :  cn1outbox event { code: 0x43524831, detail: "pop:/path"
 *                                    or "push:/path" or "replace:/path" }
 *
 * On page load the shim writes the current URL (path + search + hash) into the
 * AppArg property by sending a synthetic "replace:" message; the BrowserHistoryBridge
 * reads it via getInitialPath().
 */
(function (global) {
  "use strict";

  // The Codename One JavaScript port runs the translated bytecode inside a Web
  // Worker, and its bundler imports every .js file that lands in the build
  // output (including this one) via `importScripts`. The worker context has no
  // `document` or page-level history API, so accessing them here would crash
  // the worker before the app boots. Bail out cleanly when this shim is
  // imported anywhere other than the main browser page.
  if (typeof document === "undefined"
      || typeof global.addEventListener !== "function"
      || typeof global.history === "undefined") {
    return;
  }

  var CODE = 0x43524831; // "CRH1"

  function currentPath() {
    var loc = global.location;
    return (loc && loc.pathname ? loc.pathname : "/")
      + (loc && loc.search ? loc.search : "")
      + (loc && loc.hash ? loc.hash : "");
  }

  // App -> Shim: listen on cn1inbox for router messages.
  global.addEventListener("cn1inbox", function (ev) {
    var d = ev && ev.detail ? ev.detail : ev;
    if (!d || d.code !== CODE || typeof d.detail !== "string") return;
    var payload = d.detail;
    var colon = payload.indexOf(":");
    if (colon < 0) return;
    var verb = payload.substring(0, colon);
    var path = payload.substring(colon + 1);
    try {
      if (verb === "push") {
        global.history.pushState({ cn1: 1, path: path }, "", path);
      } else if (verb === "replace") {
        global.history.replaceState({ cn1: 1, path: path }, "", path);
      }
    } catch (e) {
      // History API can throw on file:// origins; fall back silently.
      if (global.console && global.console.warn) {
        global.console.warn("cn1-router-history: history API rejected", e);
      }
    }
  });

  // Shim -> App: forward browser back via cn1outbox.
  function emit(verb, path) {
    var msg = verb + ":" + path;
    if (typeof global.cn1OutboxDispatch === "function") {
      global.cn1OutboxDispatch({ code: CODE, detail: msg });
      return;
    }
    // Fallback: dispatch a CustomEvent the CN1 runtime listens for.
    try {
      var evt = new CustomEvent("cn1outbox", { detail: { code: CODE, detail: msg } });
      global.dispatchEvent(evt);
    } catch (_e) {
      // Older browsers without CustomEvent ctor — give up quietly.
    }
  }

  global.addEventListener("popstate", function () {
    emit("pop", currentPath());
  });

  // Seed the initial path so JsRouterBootstrap.getInitialPath() can read it.
  // We use "replace:" so the Router treats it as a same-stack location, not
  // a duplicate push.
  function seed() {
    emit("replace", currentPath());
  }
  if (document.readyState === "complete") {
    seed();
  } else {
    global.addEventListener("load", seed, { once: true });
  }
})(typeof window !== "undefined" ? window : globalThis);
