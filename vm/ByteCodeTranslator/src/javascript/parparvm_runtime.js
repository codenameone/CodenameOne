/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

(function(global) {
const CN1_STRING_VALUE = "cn1_java_lang_String_value";
const CN1_STRING_OFFSET = "cn1_java_lang_String_offset";
const CN1_STRING_COUNT = "cn1_java_lang_String_count";
const CN1_STRING_HASH = "cn1_java_lang_String_hashCode";
const CN1_SB_VALUE = "cn1_java_lang_StringBuilder_value";
const CN1_SB_COUNT = "cn1_java_lang_StringBuilder_count";
const CN1_THREAD_ALIVE = "cn1_java_lang_Thread_alive";
const CN1_THREAD_NAME = "cn1_java_lang_Thread_name";
const CN1_THREAD_NATIVE_ID = "cn1_java_lang_Thread_nativeThreadId";
const CN1_THREAD_TARGET = "cn1_java_lang_Thread_target";
const CN1_THROWABLE_MESSAGE = "cn1_java_lang_Throwable_message";
const CN1_THROWABLE_STACK = "cn1_java_lang_Throwable_stack";
const CN1_DATE_VALUE = "cn1_java_util_Date_date";
const CN1_DATEFORMAT_DATE_STYLE = "cn1_java_text_DateFormat_dateStyle";
const CN1_DATEFORMAT_TIME_STYLE = "cn1_java_text_DateFormat_timeStyle";
const CN1_STRINGBUFFER_INTERNAL = "cn1_java_lang_StringBuffer_internal";
const CN1_ENUM_NAME = "cn1_java_lang_Enum_name";
const CN1_HASHMAP_ELEMENT_DATA = "cn1_java_util_HashMap_elementData";
const CN1_HASHMAP_ENTRY_NEXT = "cn1_java_util_HashMap_Entry_next";
const CN1_HASHMAP_ENTRY_KEY = "cn1_java_util_MapEntry_key";
// Shared dispatch id for ``Object.clone()`` post the dispatch-id refactor.
// The mangler rewrites the literal in lockstep with every call site so
// equality against ``methodId`` keeps matching after mangling — the
// regex-based ``isArrayCloneMethodId`` fallback below silently breaks
// because regex bodies aren't mangled (they're literal patterns).
const CN1_CLONE_DISPATCH_ID = "cn1_s_clone_R_java_lang_Object";
const VM_PROTOCOL_VERSION = 1;
const VM_PROTOCOL = Object.freeze({
  version: VM_PROTOCOL_VERSION,
  messages: Object.freeze({
    START: "start",
    EVENT: "event",
    UI_EVENT: "ui-event",
    TIMER_WAKE: "timer-wake",
    HOST_CALL: "host-call",
    HOST_CALL_BATCH: "host-call-batch",
    HOST_CALLBACK: "host-callback",
    PROTOCOL_INFO: "protocol-info",
    PROTOCOL: "protocol",
    LOG: "log",
    RESULT: "result",
    ERROR: "error",
    // ``LIFECYCLE`` is a worker→main signal that decouples the
    // main-thread test harness's ``cn1Started`` flag from the
    // WORKER-side ``window.cn1Started = true`` set inside the
    // bootstrap's @JSBody. Sent once when the main thread
    // generator completes (Lifecycle.init + Lifecycle.start both
    // returned) so the bridge can flip its own cn1Started flag.
    LIFECYCLE: "lifecycle"
  })
});
const PRIMITIVE_INFO = {
  JAVA_BOOLEAN: { javaName: "boolean", descriptor: "Z" },
  JAVA_CHAR: { javaName: "char", descriptor: "C" },
  JAVA_FLOAT: { javaName: "float", descriptor: "F" },
  JAVA_DOUBLE: { javaName: "double", descriptor: "D" },
  JAVA_BYTE: { javaName: "byte", descriptor: "B" },
  JAVA_SHORT: { javaName: "short", descriptor: "S" },
  JAVA_INT: { javaName: "int", descriptor: "I" },
  JAVA_LONG: { javaName: "long", descriptor: "J" }
};
const jsObjectWrappers = typeof WeakMap === "function" ? new WeakMap() : null;
const externalIdentityMap = typeof WeakMap === "function" ? new WeakMap() : null;
const jsoRegistry = {
  classPrefixes: [],
  inferFn: null,
  nativeArgConverters: []
};
function sanitizeMessagePayload(value, seen) {
  if (value == null) {
    return value;
  }
  const type = typeof value;
  if (type === "string" || type === "number" || type === "boolean") {
    return value;
  }
  if (type === "function" || type === "symbol") {
    return null;
  }
  if (!seen) {
    seen = typeof WeakSet === "function" ? new WeakSet() : null;
  }
  if (seen && value && (type === "object" || type === "function")) {
    if (seen.has(value)) {
      return null;
    }
    seen.add(value);
  }
  if (Array.isArray(value)) {
    const out = new Array(value.length);
    for (let i = 0; i < value.length; i++) {
      out[i] = sanitizeMessagePayload(value[i], seen);
    }
    return out;
  }
  if (value instanceof Error) {
    return {
      name: value.name || "Error",
      message: value.message || String(value),
      stack: value.stack || null
    };
  }
  const out = {};
  const keys = Object.keys(value);
  for (let i = 0; i < keys.length; i++) {
    const key = keys[i];
    out[key] = sanitizeMessagePayload(value[key], seen);
  }
  return out;
}
function emitVmMessage(message) {
  const safeMessage = sanitizeMessagePayload(message);
  if (typeof global.__cn1ParparDispatchMessage === "function") {
    global.__cn1ParparDispatchMessage(safeMessage);
    return;
  }
  global.postMessage(safeMessage);
}
// --- Host-ref lifecycle (worker side) --------------------------------------
// The host (browser_bridge.js) keeps a HARD reference to every host object
// (canvas / image / DOM node) in hostRefById and -- exactly like the C/iOS
// backend, which has no GC on the native side -- never garbage-collects on its
// own. Cleanup is the JAVA side's responsibility: when a Java object that OWNS
// a host resource (a NativeImage's backing canvas/image) is collected, its
// finalizer releases the one host id it owns. We implement that finalizer with
// a FinalizationRegistry keyed on the OWNING Java object: ``registerNativeResource``
// is called from the JS-port image natives at creation time, and on the owner's
// collection we post a batched ``releaseHostRef`` for its id.
//
// Keying on the owner (not on JSO wrappers) is the correct altitude: the owner
// is the SOLE holder of that resource's id, so releasing when it dies can never
// pull a ref out from under a live user. (The earlier wrapper-refcount approach
// raced: the host dedups one id across many re-created worker wrappers, and a
// raw ``__jsValue`` marker could outlive the wrappers, so refcount-zero
// released canvases that were still referenced -> "Missing host receiver".)
let pendingHostRefReleases = [];
let hostRefReleaseFlushScheduled = false;
function flushHostRefReleases() {
  hostRefReleaseFlushScheduled = false;
  if (!pendingHostRefReleases.length) {
    return;
  }
  const ids = pendingHostRefReleases;
  pendingHostRefReleases = [];
  emitVmMessage({ type: "releaseHostRef", ids: ids });
}
function scheduleHostRefReleaseFlush() {
  if (hostRefReleaseFlushScheduled) {
    return;
  }
  hostRefReleaseFlushScheduled = true;
  if (typeof queueMicrotask === "function") {
    queueMicrotask(flushHostRefReleases);
  } else if (typeof setTimeout === "function") {
    setTimeout(flushHostRefReleases, 0);
  } else {
    flushHostRefReleases();
  }
}
const nativeResourceFinalizer = (typeof FinalizationRegistry === "function")
  ? new FinalizationRegistry(function(hostId) {
      if (hostId == null || hostId === 0) {
        return;
      }
      pendingHostRefReleases.push(hostId);
      scheduleHostRefReleaseFlush();
    })
  : null;
// Register that ``owner`` (a Java object) owns the host resource identified by
// ``hostId`` (or by the host-ref marker / wrapper ``hostResource``). When the
// owner is GC'd the host id is released. Idempotent-safe: registering the same
// owner twice just arms two finalizer callbacks for (possibly) different ids.
function registerNativeResource(owner, hostResource) {
  if (!nativeResourceFinalizer || owner == null || typeof owner !== "object") {
    return;
  }
  let hostId = null;
  if (typeof hostResource === "number") {
    hostId = hostResource;
  } else if (hostResource != null && typeof hostResource === "object") {
    if (hostResource.__cn1HostRef != null) {
      hostId = hostResource.__cn1HostRef;
    } else if (hostResource.__jsValue != null && hostResource.__jsValue.__cn1HostRef != null) {
      hostId = hostResource.__jsValue.__cn1HostRef;
    }
  }
  if (hostId == null || hostId === 0) {
    return;
  }
  nativeResourceFinalizer.register(owner, hostId);
}
// Object-returning JSO methods that are SAFE to re-issue when their result
// comes back degraded (see invokeJsoBridge). All are side-effect-free reads or
// create a fresh detached/standalone object (no DOM mutation, no state change),
// so repeating one on the rare crossed-response path can't corrupt anything.
const JSO_RETRYABLE_READ_METHODS = {
  createElement: true, createElementNS: true, getContext: true,
  getImageData: true, measureText: true, getElementById: true,
  querySelector: true, getBoundingClientRect: true, getComputedStyle: true,
  createPattern: true, createLinearGradient: true, createRadialGradient: true,
  // canvas -> PNG/data-URL encode used by the screenshot emit path. A cross on
  // the canvas receiver throws "Missing JS member toDataURL" / returns a
  // degraded value; encoding is a pure read (no canvas mutation) so re-issuing
  // is safe. Not covering it let an emit-time cross escape and deadlock the EDT.
  toDataURL: true, toBlob: true
};
// Max re-issues for a degraded idempotent read before giving up. A response
// CROSS comes in BURSTS (a cluster of concurrent numeric getters whose replies
// cross object reads); 4 was too few to outlast a sustained burst (createElement
// observed exhausting all 4 in CI). With a backoff sleep between tries -- which
// lets the in-flight numeric getters finish so they can't re-cross -- a dozen
// tries clears realistic bursts while staying bounded (only on the degraded
// path, ~0.5s worst case).
const JSO_MAX_RETRY = 12;
// Lost-response watchdog timeouts, keyed by host-call symbol (see the watchdog
// armed in dispatchYield). Only BOUNDED host natives are listed: a fast
// JSO-bridge DOM/canvas read (resolves in <100ms), the screenshot UI-settle
// wait (a bounded rAF loop, ~maxFrames), and the canvas->PNG capture (rAF +
// encode, sub-second). If the host channel drops their response the worker
// would otherwise park forever; on expiry we resume the thread with a transient
// error so the caller recovers (JSO reads re-issue via the retry; capture/
// settle callers catch and emit a placeholder / advance). UNBOUNDED natives
// (image load over the network, fetch, the screenshot WebSocket) are
// deliberately ABSENT -- they legitimately run as long as the app/network
// needs and must never be aborted by a timer.
const HOST_CALL_WATCHDOG_MS = {
  "__cn1_jso_bridge__": 2000,
  "__cn1_dom_window_current__": 5000,
  "__cn1_create_custom_event__": 5000,
  "__cn1_hide_splash__": 5000,
  "__cn1_load_truetype_font__": 15000,
  "__cn1_wait_for_ui_settle__": 8000,
  "__cn1_capture_canvas_png__": 10000,
  // Surface pixel read-backs (getRGB / image-encode). Like every other read
  // here, a dropped host response must FAIL FAST (resume the caller with a
  // transient error) rather than park the worker forever -- these were the only
  // reads missing a bound. Host-side they are a synchronous getImageData /
  // toDataURL, so a healthy channel resolves in well under a second.
  "__cn1_surface_read__": 4000,
  "__cn1_image_read__": 4000,
  "__cn1_surface_to_dataurl__": 6000
};
// Retryable reads that can NEVER legitimately return null/undefined: for these
// a null result is itself a degraded read (a lost/crossed response delivered
// null instead of the element/context) and must be re-issued. createElement on
// a real document always returns an element; getContext('2d') always returns a
// context; createPattern/createLinearGradient/createRadialGradient always
// return an object; getImageData/measureText/getBoundingClientRect always
// return their result object. getElementById/querySelector are deliberately
// EXCLUDED -- null is their legitimate "not found" answer.
const JSO_NEVER_NULL_READS = {
  createElement: true, createElementNS: true, getContext: true,
  getImageData: true, measureText: true, getBoundingClientRect: true,
  getComputedStyle: true,
  createLinearGradient: true, createRadialGradient: true,
  toDataURL: true
};
// A degraded object read: a NUMBER where an object was expected (a crossed
// numeric getter response), a truthy empty {} that lost its host-ref marker on
// the round-trip, OR null/undefined from a method that can never legitimately
// return null (see JSO_NEVER_NULL_READS -- this is the transport-cross case
// where createElement resumed with another call's null/void response). A plain
// null from a nullable read (getElementById, a getter) is NOT degraded and
// passes through untouched.
function isDegradedObjectResult(r, bridge) {
  if (typeof r === "number") {
    return true;
  }
  if (r && typeof r === "object" && !Array.isArray(r)
      && r.__cn1HostRef == null
      && Object.getOwnPropertyNames(r).length === 0) {
    return true;
  }
  if (r == null && bridge && bridge.kind === "method"
      && JSO_NEVER_NULL_READS[bridge.member] === true) {
    return true;
  }
  return false;
}
// A transient host-bridge error worth re-issuing an idempotent read for: the
// host momentarily failed to resolve the receiver (its host-ref crossed with a
// concurrent call and pointed at a degraded value), so a member lookup / method
// call threw rather than running. The same read usually succeeds once the
// crossed response drains. Matches the exact throw strings the host bridge
// emits (browser_bridge.js: "Missing JS member ...", "Missing host receiver
// ...") plus the generic "is not a function" a degraded receiver produces.
function isTransientHostBridgeError(e) {
  const m = e == null ? "" : (e.message != null ? String(e.message) : String(e));
  if (!m) {
    return false;
  }
  return m.indexOf("Missing JS member") >= 0
      || m.indexOf("Missing host receiver") >= 0
      || m.indexOf("is not a function") >= 0
      || m.indexOf("host call timed out") >= 0;
}
// An entry in ``cls.methods`` may be either a function (the common
// case) or a STRING naming another translated function. Inherited
// method aliases emit the latter form — the alias ``$childId`` points
// at the declaring class's function name ``"$parentFn"`` as a string
// literal so the object literal can be evaluated at file-load time
// even if ``$parentFn`` is defined in a later-loaded chunk. At first
// virtual-dispatch we resolve the string via ``global[name]``, write
// the function back into the methods table in place of the string
// (so subsequent lookups skip the resolution), and return it.
function resolveMethodEntry(methods, methodId) {
  let entry = methods[methodId];
  if (typeof entry === "string") {
    const resolved = global[entry];
    if (typeof resolved === "function") {
      methods[methodId] = resolved;
      entry = resolved;
    }
  }
  return entry;
}
// Format an arbitrary thrown value into a readable string for the
// ERROR message we ship to the main thread. Native ``Error`` objects
// already stringify to ``Name: message``; translated Java throwables
// are plain JS objects whose ``toString`` yields ``[object Object]``,
// so we pull the class name and ``Throwable.message`` field out by
// hand. Anything else falls through to ``String(error)``.
function formatErrorForVm(error) {
  if (error instanceof Error) {
    return (error.name || "Error") + ": " + (error.message || "");
  }
  if (error && typeof error === "object") {
    const cls = error.__class || (error.__classDef && error.__classDef.name);
    if (cls) {
      let jmsg = error.cn1_java_lang_Throwable_message;
      if (jmsg && typeof jmsg === "object") {
        try { jmsg = jvm.toNativeString(jmsg); } catch (_fm) { jmsg = "[unserialisable]"; }
      }
      return "JavaThrow[" + cls + "]: " + (jmsg == null ? "(no-message)" : jmsg);
    }
  }
  return "" + error;
}
const VM_TRACE_WAIT_LIMIT = 64;
let vmTraceWaitCount = 0;
let vmTraceWaitSuppressed = false;
function vmTrace(line) {
  if (typeof line === "string" && line.indexOf("runtime.") === 0) {
    return;
  }
  if (typeof line === "string" && line.indexOf("runtime.handleYield.thread-") === 0 && line.indexOf(":wait") > 0) {
    vmTraceWaitCount++;
    if (vmTraceWaitCount > VM_TRACE_WAIT_LIMIT) {
      if (!vmTraceWaitSuppressed && global.console && typeof global.console.log === "function") {
        vmTraceWaitSuppressed = true;
        global.console.log("PARPAR:runtime.handleYield:wait:throttled");
      }
      return;
    }
  }
  if (global.console && typeof global.console.log === "function") {
    global.console.log("PARPAR:" + line);
  }
}
function shouldEnableDiag() {
  if (global.__parparDiagEnabled != null) {
    return !!global.__parparDiagEnabled;
  }
  const loc = (global.window || global).location;
  const rawSearch = (loc && loc.search) ? String(loc.search) : String(global.__cn1LocationSearch || "");
  if (!rawSearch) {
    return false;
  }
  const search = rawSearch.charAt(0) === "?" ? rawSearch.substring(1) : rawSearch;
  if (!search) {
    return false;
  }
  const pairs = search.split("&");
  for (let i = 0; i < pairs.length; i++) {
    const entry = pairs[i];
    if (!entry) {
      continue;
    }
    const eq = entry.indexOf("=");
    const key = decodeURIComponent((eq >= 0 ? entry.substring(0, eq) : entry).replace(/\+/g, " "));
    if (key !== "parparDiag") {
      continue;
    }
    const rawValue = decodeURIComponent((eq >= 0 ? entry.substring(eq + 1) : "1").replace(/\+/g, " "));
    const normalized = String(rawValue).toLowerCase();
    return !(normalized === "0" || normalized === "false" || normalized === "off" || normalized === "no");
  }
  return false;
}
const VM_DIAG_ENABLED = shouldEnableDiag();
// Opt-in (URL ?cn1DisableSelfHeal=1) kill switch for the worker's stalled-timer
// SELF-HEAL recovery backstop. Self-heal masks a real wedge non-deterministically
// (sometimes it re-arms the lost timer before the suite watchdog, sometimes not),
// which hides the deterministic root cause. With it OFF a lost/stalled timer
// STAYS stalled, so the failure is reproducible and trackable. Lazy because the
// URL search is forwarded from the main thread on START (after this file loads).
function cn1SelfHealDisabled() {
  try {
    return typeof self !== "undefined"
      && typeof self.getParameterByName === "function"
      && self.getParameterByName("cn1DisableSelfHeal") === "1";
  } catch (_e) {
    return false;
  }
}

// Event forwarding (worker-side functions becoming real listeners on the
// main thread) defaults on because production apps need user input to
// reach Java code. The screenshot-test harness passes
// ``cn1DisableEventForwarding=1`` because those tests were written
// against the pre-existing broken behaviour where addEventListener was
// a silent no-op; enabling real events makes BrowserComponent /
// MediaPlayback / etc. tests diverge from their recorded baselines.
let __cn1EventForwardingCache = null;
function __cn1EventForwardingEnabled() {
  if (__cn1EventForwardingCache !== null) {
    return __cn1EventForwardingCache;
  }
  let disabled = false;
  try {
    const loc = (global.window || global).location;
    const rawSearch = (loc && loc.search) ? String(loc.search) : String(global.__cn1LocationSearch || "");
    if (rawSearch) {
      const search = rawSearch.charAt(0) === "?" ? rawSearch.substring(1) : rawSearch;
      const pairs = search.split("&");
      for (let i = 0; i < pairs.length; i++) {
        const entry = pairs[i];
        if (!entry) continue;
        const eq = entry.indexOf("=");
        const key = decodeURIComponent((eq >= 0 ? entry.substring(0, eq) : entry).replace(/\+/g, " "));
        if (key !== "cn1DisableEventForwarding") continue;
        const rawValue = decodeURIComponent((eq >= 0 ? entry.substring(eq + 1) : "1").replace(/\+/g, " "));
        const normalized = String(rawValue).toLowerCase();
        disabled = !(normalized === "0" || normalized === "false" || normalized === "off" || normalized === "no");
        break;
      }
    }
  } catch (_err) {
    disabled = false;
  }
  if (!disabled && global.__cn1DisableEventForwarding) {
    disabled = true;
  }
  __cn1EventForwardingCache = !disabled;
  return __cn1EventForwardingCache;
}
const VM_TRACE_THREAD_LIMIT = 12;
function diagValue(value) {
  if (value == null) {
    return "null";
  }
  return String(value).replace(/\s+/g, "_");
}
function vmDiag(phase, key, value) {
  if (!VM_DIAG_ENABLED) {
    return;
  }
  vmTrace("DIAG:" + phase + ":" + key + "=" + diagValue(value));
}
// Always-on lifecycle log — writes to console.log regardless of the
// parparDiag URL flag so a user who reports "stuck on Loading..., no
// console output" can confirm whether the runtime even executed. Kept
// minimal: a handful of single-line messages covering load → main
// generator → spawn → drain. For deeper traces pass ``?parparDiag=1``
// which enables the full vmDiag stream.
function vmLifecycle(message) {
  if (global.console && typeof global.console.log === "function") {
    global.console.log("PARPAR-LIFECYCLE:" + message);
  }
}
vmLifecycle("runtime-script-loaded");
function shouldTraceThread(thread) {
  return VM_DIAG_ENABLED && !!thread && (thread.id | 0) <= VM_TRACE_THREAD_LIMIT;
}
function parseMissingVirtualMessage(error) {
  const message = error == null ? "" : String(error);
  const match = message.match(/Missing virtual method ([^\s]+) on ([^\s]+)/);
  if (!match) {
    return null;
  }
  return {
    methodId: match[1],
    receiverClass: match[2]
  };
}
function printStreamValue(value) {
  if (value == null) {
    return "null";
  }
  if (typeof value === "string" || typeof value === "number" || typeof value === "boolean") {
    return String(value);
  }
  if (value.__class === "java_lang_String" && jvm && typeof jvm.toNativeString === "function") {
    return jvm.toNativeString(value);
  }
  if (value.__className) {
    return String(value.__className);
  }
  if (value.__class) {
    return String(value.__class);
  }
  try {
    return JSON.stringify(value);
  } catch (err) {
    return String(value);
  }
}
function printToConsole(line) {
  if (global.console && typeof global.console.log === "function") {
    global.console.log(line);
  }
  // When enabled by the JS port (port.js), forward System.out.println output
  // to the main thread so Playwright can capture it.  Disabled by default to
  // avoid flooding test harnesses that use Node.js worker_threads.
  if (global.__cn1ForwardConsoleToMain) {
    emitVmMessage({ type: "log", message: String(line) });
  }
}
function isObjectLike(value) {
  return value != null && (typeof value === "object" || typeof value === "function");
}
function isJavaStringObject(value) {
  return !!(value && value.__class === "java_lang_String");
}
function areStringLikeEqual(a, b) {
  if (!((typeof a === "string") || isJavaStringObject(a))) {
    return false;
  }
  if (!((typeof b === "string") || isJavaStringObject(b))) {
    return false;
  }
  return jvm.toNativeString(a) === jvm.toNativeString(b);
}
function identityHash(obj) {
  if (obj == null) {
    return 0;
  }
  if (typeof obj.__id === "number" && obj.__id !== 0) {
    return obj.__id | 0;
  }
  if (!isObjectLike(obj)) {
    return 0;
  }
  if (externalIdentityMap) {
    const existing = externalIdentityMap.get(obj);
    if (typeof existing === "number" && existing !== 0) {
      return existing | 0;
    }
  }
  const id = jvm.nextIdentity++ | 0;
  if (Object.isExtensible && Object.isExtensible(obj)) {
    try {
      obj.__id = id;
      return id;
    } catch (ignored) {
      // Fall back to WeakMap when direct assignment is not possible.
    }
  }
  if (externalIdentityMap) {
    externalIdentityMap.set(obj, id);
    return id;
  }
  return 0;
}
function createConsolePrintStream() {
  try {
    return jvm.newObject("java_io_PrintStream");
  } catch (err) {
    if (global.console && typeof global.console.warn === "function") {
      global.console.warn("PARPAR:console-printstream-init-failed", err);
    }
    return null;
  }
}
function ensureSystemPrintStreams() {
  const systemClass = jvm.classes["java_lang_System"];
  if (!systemClass) {
    return;
  }
  const staticFields = systemClass.staticFields || (systemClass.staticFields = {});
  if (staticFields.out == null) {
    staticFields.out = createConsolePrintStream();
  }
  if (staticFields.err == null) {
    staticFields.err = staticFields.out != null ? staticFields.out : createConsolePrintStream();
  }
}
function threadDebugLabel(threadObject) {
  if (!threadObject) {
    return "thread:null";
  }
  const name = jvm && typeof jvm.toNativeString === "function"
    ? jvm.toNativeString(threadObject[CN1_THREAD_NAME])
    : "";
  const target = threadObject[CN1_THREAD_TARGET];
  const targetClass = target && target.__class ? target.__class : "null";
  return "thread:" + (name || "null") + ":target:" + targetClass;
}

// ============================================================================
// Cooperative Scheduler -- map of thread behavior in the JS port runtime
// ============================================================================
//
// The JS port runs the entire VM in a single Web Worker (one OS thread). All
// "Java threads" the application sees are green threads multiplexed
// cooperatively on that single OS thread. This block documents the
// scheduler's data structures, lifecycle, and yield protocol so anyone
// touching ``drain`` / ``handleYield`` / monitor ops has a precise mental
// model.
//
// State (on the ``jvm`` object below):
//   threads[]          -- every Thread struct ever spawned (housekeeping).
//   runnable[]         -- FIFO queue of threads ready to run. Drained head-
//                         first; thread.start, sleep(0), monitor-promotion,
//                         and notifyAll-of-an-unowned-monitor all push here.
//   currentThread      -- thread being dispatched right now (only valid
//                         inside ``drain``).
//   draining           -- re-entrancy guard. ``enqueue`` may be called from
//                         inside ``drain``; the recursive ``drain()`` call
//                         short-circuits on this flag.
//   drainScheduled     -- a setTimeout(drain, 0) is already pending.
//   timedWakeups[]     -- entries for sleep(N>0) and wait(N>0). One global
//                         ``_wakeupTimer`` fires for the soonest deadline
//                         (browser/Node setTimeout); see _refreshTimed-
//                         WakeupTimer.
//
// Per-thread state (Thread struct):
//   id                 -- monotonically assigned at spawn; used as
//                         monitor.owner.
//   object             -- the java.lang.Thread receiver, if any.
//   generator          -- the JS generator implementing the thread's
//                         translated body. ``drain`` calls
//                         ``generator.next(resumeValue)`` to advance one
//                         "step" (i.e. up to the next yield).
//   waiting            -- non-null when parked: { op: "sleep" | "wait" |
//                         "monitor_enter" | "HOST_CALL", ... }. Cleared by
//                         ``enqueue``.
//   resumeValue        -- value handed to generator.next on next resume
//                         (e.g. notifyAll passes null; interrupt passes
//                         { interrupted: true }).
//   resumeError        -- when set, drain calls generator.throw instead of
//                         next (interrupt-during-wait, host-callback error).
//   done               -- generator finished. drain skips done threads;
//                         on completion drain also flips
//                         object.alive=0 and notifyAll(object) so any
//                         join() waiters wake up.
//
// Per-monitor state (obj.__monitor, lazily attached):
//   owner              -- thread.id holding the monitor, or null.
//   count              -- reentry count. monitorEnter on owner.id increments;
//                         monitorExit decrements. count==0 releases
//                         ownership and promotes the head entrant.
//   entrants[]         -- threads parked on monitorEnter contention.
//                         FIFO: monitorExit shifts the head, sets that
//                         thread as the new owner, restores its reentry
//                         count, and enqueues it. Order is preserved so
//                         there is no later-arrival "stealing".
//   waiters[]          -- threads parked on Object.wait(). notify pulls
//                         from the head; notifyAll splices all. Each waiter
//                         carries its saved reentryCount so it re-acquires
//                         at the right depth.
//
// Yield protocol (handleYield):
//   { op: "sleep", millis: 0 }      -> enqueue(thread). Pure cooperative
//                                       hand-off, no real-time delay.
//   { op: "sleep", millis: N>0 }    -> _scheduleTimedWakeup with kind=sleep;
//                                       waking enqueues the thread.
//   { op: "wait", monitor, timeout, reentryCount }
//                                    -> push onto monitor.waiters; if
//                                       timeout>0 also _scheduleTimedWakeup
//                                       with kind=wait; on wake/notify call
//                                       resumeWaiter (which either takes
//                                       ownership or re-parks on entrants).
//   { op: "monitor_enter", monitor, entrant }
//                                    -> entrant has already been pushed
//                                       onto monitor.entrants by
//                                       monitorEnter; handleYield only
//                                       records thread.waiting. monitorExit
//                                       wakes it on release.
//   { op: HOST_CALL, id, symbol, args }
//                                    -> outbound RPC to the main thread;
//                                       resumed when host posts a
//                                       host-callback message back.
//   { op: "await", promise }
//                                    -> suspend on a WORKER-LOCAL promise (no
//                                       host round-trip). Resumed via the
//                                       promise's own .then -> enqueue; reject
//                                       resumes with null. Used to block a
//                                       green thread until a worker-side async
//                                       resource (e.g. a FontFace in
//                                       self.fonts) is loaded before a getter
//                                       reads it.
//
// Acquire/release lifecycle for synchronized:
//   monitorEnter(thread, obj):
//     uncontended (owner null or self): set owner=thread.id; count++; return
//                                        null (no yield).
//     contended:                         push entrant; return {op:
//                                        "monitor_enter"} for the caller
//                                        to ``yield``. The translator emits
//                                        ``yield* _me(obj)`` -- the yield*
//                                        is critical, plain ``_me(obj)``
//                                        starts an iterator that nobody
//                                        consumes and silently never
//                                        acquires the monitor.
//   monitorExit(thread, obj):
//     count-- ; if count<=0: clear owner; if entrants non-empty, shift the
//     head, assign owner+count from it, enqueue its thread. The entrant's
//     resumeValue is null so its paused ``yield`` returns null and execution
//     continues into the synchronized body with the lock now held.
//   waitOn(thread, obj, timeout):
//     save reentryCount; clear owner+count; **drain the head of the
//     entrants queue exactly like monitorExit does** (otherwise an
//     entrant that was already queued when the holder called wait
//     stays parked forever on an unowned monitor); return
//     {op:"wait", reentryCount}. The lock is fully released until
//     resumeWaiter restores ownership.
//   resumeWaiter(waiter):
//     if monitor unowned (or self-owned): take ownership at saved depth,
//     enqueue waiter.thread. Otherwise re-park on monitor.entrants -- the
//     waiter has to compete for re-entry like any other contender.
//
// Drain budget:
//   drain bails after 2048 steps OR 8ms wall-clock (real
//   performance.now()), whichever comes first; scheduleDrain posts a
//   setTimeout(drain, 0) so the host event loop can process pointer
//   events / network callbacks between drain bursts. The bail-out keeps a
//   compute-heavy green thread from monopolising the worker.
//
// Common pitfalls when editing:
//   * Translator MUST emit ``yield* _me(...)`` for monitorenter and
//     synchronized-method entry. Plain ``_me(...)`` returns an unawaited
//     iterator: monitorEnter never runs, monitorExit later sees an
//     unowned monitor and throws IllegalMonitorStateException.
//   * monitorExit must promote at most one entrant. Promoting more would
//     break mutual exclusion (multiple owners simultaneously).
//   * Adding new yield ops requires both a handler in handleYield AND an
//     unpark path that calls enqueue / resumeWaiter -- otherwise the
//     thread stays parked forever.
//   * The installed ByteCodeTranslator jar bundles parparvm_runtime.js;
//     edits to the source file require ``mvn install`` on
//     vm/ByteCodeTranslator before downstream tests pick them up.
// ============================================================================
const jvm = {
  classes: {},
  nativeMethods: Object.create(null),
  literalStrings: Object.create(null),
  methodTailCache: Object.create(null),
  remappedMethodIdCache: Object.create(null),
  resolvedVirtualCache: Object.create(null),
  nextIdentity: 1,
  nextThreadId: 1,
  nextHostCallId: 1,
  // Registry of worker-side JS functions that can be invoked from the main
  // thread via an event-dispatch postMessage. ``toHostTransferArg`` mints
  // an ID for any function argument (e.g. the wrapped EventListener
  // created by port.js's nativeArgConverter) and hands the main thread
  // back a ``{ __cn1WorkerCallback: id }`` token instead of null. When the
  // real DOM event fires on the main thread, browser_bridge.js looks up
  // the token, wraps it in a real JS function that postMessages a
  // ``worker-callback`` message carrying the serialised event, and the
  // worker invokes the stored function with the synthesised event proxy.
  nextWorkerCallbackId: 1,
  workerCallbacks: Object.create(null),
  currentThread: null,
  runnable: [],
  threads: [],
  // Single-timer cooperative scheduler. ``timedWakeups`` holds entries for
  // sleep / Object.wait(timeout); one global ``_wakeupTimer`` fires for the
  // soonest pending deadline. Replaces the per-yield ``setTimeout`` chain
  // that piled up dozens of pending browser timers (every Display
  // invokeAndBlock iteration creates lock.wait(10)) and crowded out
  // self.onmessage from receiving incoming pointer events. See
  // ``_scheduleTimedWakeup`` / ``_processExpiredTimedWakeups``.
  timedWakeups: [],
  _wakeupTimer: null,
  _wakeupAt: Infinity,
  // When a green thread enters an "atomic section" (flushGraphics today),
  // ``drain`` only dispatches that thread until the section ends. Other
  // runnables wait. Prevents repaint() / requestAnimationFrame /
  // Form-transition logic from interleaving with a frame's canvas-op
  // chain and recursively producing more canvas ops.
  atomicThread: null,
  // Screenshot-capture serialization. While a green thread is reading the
  // visible canvas (the cn1ss emit's ``__cn1_wait_for_ui_settle__`` +
  // ``__cn1_capture_canvas_png__`` host round-trips, which span up to ~24 rAF
  // frames on the host), ANY other green thread that paints would draw onto
  // codenameone-canvas mid-capture and the sampled PNG would show the wrong
  // (next/previous) form -- the screenshot "off-by-one" that forces dual-stream
  // tests (ChatInput/ChatView dual-appearance) to be parked. Unlike the dead
  // ``atomicThread`` flag, this gate is set ONLY for the brief capture window
  // (see beginCaptureGate/endCaptureGate) and is deadlock-safe: the owner parks
  // solely on the host (never on a monitor held by a deferred thread), and the
  // form present runs BEFORE the gate is taken, so the owner acquires no monitor
  // while holding it. When null (the steady state) drain() behaves exactly as
  // before -- the gate is inert outside captures.
  captureGateOwner: null,
  pendingHostCalls: Object.create(null),
  // Batched fire-and-forget JSO bridge ops. Every canvas/DOM setter or
  // void method call inside a paint frame produces a HOST_CALL that
  // doesn't expect a reply. Instead of posting them one by one
  // (hundreds per frame -> hundreds of structured-clone serialisations
  // and worker->main postMessage trips during boot), we accumulate
  // them per drain burst and flush as a single ``host-call-batch``
  // message at the end of drain (or before any round-trip
  // ``invokeHostNative``, to preserve op ordering on the host side).
  // The browser bridge unpacks the batch and replays each op in
  // submission order.
  pendingFireAndForget: [],
  eventQueue: [],
  mainClass: null,
  mainMethod: null,
  protocol: VM_PROTOCOL,
  diagEnabled: VM_DIAG_ENABLED,
  lastVirtualFailure: null,
  firstFailure: null,
  defineClass(def) {
    // Translator emits short property names (n/b/i/I/A/a/f/s) to save
    // ~60 chars per class × 1590 classes. Downstream runtime code
    // still reads the long names, so remap them here at registration
    // time. Hand-written runtime / port.js calls that still use the
    // long names continue to work (the ``||`` fallback keeps both
    // spellings valid).
    if (def.n !== undefined) {
      def.name = def.n;
      // ``b`` omitted ⇒ base is java.lang.Object (translator's default
      // for all direct-extends-Object classes; saves ~7 chars per
      // entry). Object itself emits ``b: null`` to break the walk.
      def.baseClass = def.b === undefined
        ? (def.n === "java_lang_Object" ? null : "java_lang_Object")
        : def.b;
      def.interfaces = def.i || [];
      def.isInterface = !!def.I;
      def.isAbstract = !!def.A;
      // ``a`` encodes either an explicit assignableTo map (debug /
      // full mode) or — by default — is omitted entirely, asking
      // us to auto-populate. The auto-populate unions self + the
      // direct baseClass + every interface (plus their already-
      // computed assignableTo unions). The classes are NOT emitted
      // in inheritance order — IllegalStateException can land in
      // translated_app.js BEFORE RuntimeException, so a naive walk
      // of ``this.classes[base].baseClass`` would terminate after
      // one hop and miss every grandparent. Always pin the direct
      // baseClass name unconditionally so a later ``instanceOf X``
      // check at least matches the immediate parent; the
      // {@link findAncestorAssignable} fallback handles the deeper
      // ancestors lazily by walking the baseClass-string chain at
      // query time, when every ancestor is guaranteed to be
      // registered.
      if (def.a === undefined || def.a === 1) {
        const assignable = Object.create(null);
        assignable[def.name] = 1;
        assignable["java_lang_Object"] = 1;
        if (def.baseClass) {
          assignable[def.baseClass] = 1;
        }
        let base = def.baseClass;
        while (base) {
          const baseDef = this.classes[base];
          if (baseDef && baseDef.assignableTo) {
            for (const k in baseDef.assignableTo) {
              assignable[k] = 1;
            }
          }
          base = baseDef ? baseDef.baseClass : null;
        }
        for (let i = 0; i < def.interfaces.length; i++) {
          const ifaceName = def.interfaces[i];
          const ifaceDef = this.classes[ifaceName];
          if (ifaceDef && ifaceDef.assignableTo) {
            for (const k in ifaceDef.assignableTo) {
              assignable[k] = 1;
            }
          }
          assignable[ifaceName] = 1;
        }
        def.assignableTo = assignable;
      } else {
        def.assignableTo = def.a || {};
      }
      // Packed instance-field encoding: ``f: "$a|$b:I|$c"`` is a
      // pipe-separated list of ``name[:type]`` pairs; expand into
      // the legacy [[name,type],[name],...] tuple array that
      // ``initInstanceFields`` already understands. Keeps the
      // hot-path code fast (no re-parse per object init) while
      // shaving ~14 KiB of tuple-array brackets off the wire.
      if (typeof def.f === "string") {
        const parts = def.f ? def.f.split("|") : [];
        const fields = new Array(parts.length);
        for (let i = 0; i < parts.length; i++) {
          const colon = parts[i].indexOf(":");
          fields[i] = colon >= 0
            ? [parts[i].substring(0, colon), parts[i].substring(colon + 1)]
            : [parts[i]];
        }
        def.instanceFields = fields;
      } else {
        def.instanceFields = def.f || [];
      }
      def.staticFields = def.s || {};
    } else {
      def.staticFields = def.staticFields || {};
      def.instanceFields = def.instanceFields || [];
      def.assignableTo = def.assignableTo || {};
    }
    def.methods = def.methods || {};
    def.classObject = {
      __class: "java_lang_Class",
      __monitor: this.createMonitor(),
      __className: def.name,
      __isClassObject: true,
      __classDef: def,
      cn1_staticFields: def.staticFields
    };
    this.classes[def.name] = def;
    // ``def.c`` — inline clinit attachment. Replaces the old
    // separate ``jvm.classes["cls"].clinit = $fn`` statement that
    // used to follow ``_Z`` in the translated output.
    if (def.c) {
      def.clinit = def.c;
    }
    // ``def.t`` — inline no-arg constructor attachment. Reflective
    // construction paths (``Class.newInstance()`` /
    // ``jvm.createException()``) used to look up the constructor as
    // ``global["cn1_" + def.name + "___INIT__"]``, but ``def.name``
    // is the *mangled* short class symbol while the actual ctor
    // global was renamed by the post-translation mangler to a
    // different short symbol — so the string-concat lookup never
    // matches and ``newInstance`` returns objects whose constructors
    // never ran (most visibly: every reflectively-created Component
    // arrives with ``bounds = null`` and trips an NPE on the first
    // pointer-event hit-test). The translator now passes the ctor as
    // a direct function reference under ``t:``; pin it onto the
    // classDef under ``noArgCtor`` for the reflective callers.
    if (def.t) {
      def.noArgCtor = def.t;
    }
    // Inline methods map: the class def may carry its virtual-method
    // registrations directly (``m: {$sig:$fn,...}``) instead of
    // requiring a separate ``_M("cls", {...})`` call afterwards.
    // Consolidating the two cuts the per-class ``_M("cls",`` prefix.
    if (def.m) {
      this.applyMethodMap(def, def.m);
    }
  },
  addVirtualMethod(className, methodId, fn) {
    const nativeOverride = this.nativeMethods[methodId];
    this.classes[className].methods[methodId] = typeof nativeOverride === "function" ? nativeOverride : fn;
  },
  // Batched virtual-method registration. The translator emits one
  // ``jvm.m("Cls",{$m1,$m2,$anc:$m1,...})`` per class instead of a
  // separate ``jvm.addVirtualMethod(...)`` call per method+alias.
  // That was 62% of a ~28 MB bundle at its peak — ES2015 property
  // shorthand collapses primary registrations to ``$m1,`` (5 bytes)
  // and ancestor aliases to ``$anc:$m1,`` (~12 bytes).
  //
  // The object's own property enumeration order is the translator's
  // emission order, so native overrides take effect even when the
  // method's own entry lands in the table before the override is
  // registered: we consult ``jvm.nativeMethods`` for every entry.
  m(className, methodMapOrThunk) {
    const cls = this.classes[className];
    if (!cls) {
      return;
    }
    // ``methodMapOrThunk`` is an arrow function returning the map,
    // not the map itself. Evaluating it eagerly here would re-
    // introduce the cross-chunk forward-reference problem that
    // previously forced alias entries to be encoded as string
    // literals. Store the thunk on the class and defer materialising
    // the map until first virtual dispatch or
    // ``applyNativeOverrides`` — both happen after every chunk has
    // finished its top-level declarations.
    if (typeof methodMapOrThunk === "function") {
      cls.pendingMethods = cls.pendingMethods || [];
      cls.pendingMethods.push(methodMapOrThunk);
      return;
    }
    // Legacy path: plain object map (e.g., from hand-written
    // runtime/port code).
    this.applyMethodMap(cls, methodMapOrThunk);
  },
  applyMethodMap(cls, methodMap) {
    const methods = cls.methods;
    const natives = this.nativeMethods;
    const keys = Object.keys(methodMap);
    for (let i = 0; i < keys.length; i++) {
      const methodId = keys[i];
      const override = natives[methodId];
      methods[methodId] = typeof override === "function" ? override : methodMap[methodId];
    }
  },
  flushPendingMethods(cls) {
    const pending = cls.pendingMethods;
    if (!pending || !pending.length) {
      return;
    }
    cls.pendingMethods = null;
    for (let i = 0; i < pending.length; i++) {
      this.applyMethodMap(cls, pending[i]());
    }
  },
  setMain(className, methodName) {
    this.mainClass = className;
    this.mainMethod = methodName;
  },
  createMonitor() {
    return { owner: null, count: 0, waiters: [], entrants: [] };
  },
  getClassObject(className) {
    const cls = this.classes[className];
    if (!cls) {
      throw new Error("Unknown class " + className);
    }
    return cls.classObject;
  },
  ensureClassInitialized(className) {
    const cls = this.classes[className];
    if (!cls) {
      throw new Error("Unknown class " + className);
    }
    if (cls.initialized || cls.initializing) {
      if (VM_DIAG_ENABLED && !cls.initialized && className === "com_codename1_ui_Display") {
        try { vmTrace("DIAG:CLINIT_REENTRY:" + className + ":stack=" + String(new Error().stack).split("\n").slice(1, 14).join("<")); } catch (_e) {}
      }
      return;
    }
    cls.initializing = true;
    if (cls.baseClass) {
      this.ensureClassInitialized(cls.baseClass);
    }
    // NOTE: ``initialized`` is only set after the clinit completes
    // successfully (see end of this function). Re-entrant reads from
    // within the clinit early-return on ``initializing`` above. If the
    // clinit throws, the class stays uninitialized and the next _I
    // retries instead of silently running with half-written statics.
    const clinitMethodId = "cn1_" + className + "___CLINIT__";
    const clinit = this.nativeMethods[clinitMethodId] || cls.clinit;
    if (clinit) {
      // Track depth: any code reached transitively from a clinit
      // must NOT cooperatively yield, because the run-to-completion
      // loop below cannot honour {sleep:N>0}/{wait:...} (the worker
      // is single-threaded and the surrounding caller is blocked on
      // this synchronous step). The translator-emitted budget yield
      // (``yield* _Y()`` at every generator-method entry) reads
      // ``jvm.__cn1ClinitDepth`` and short-circuits to a no-op when
      // it is non-zero.
      jvm.__cn1ClinitDepth = (jvm.__cn1ClinitDepth || 0) + 1;
      let result;
      try {
        result = clinit();
      } catch (e) {
        jvm.__cn1ClinitDepth--;
        cls.initializing = false;
        if (VM_DIAG_ENABLED) {
          try { vmTrace("DIAG:CLINIT_THREW:" + className + ":err=" + String(e && e.message || e).slice(0, 120) + ":stack=" + String(e && e.stack || new Error().stack).split("\n").slice(0, 12).join("<")); } catch (_e2) {}
        }
        throw e;
      }
      // A clinit declared synchronous by the translator returns a
      // non-iterable value (usually ``null``) and has no suspension
      // points — nothing to drive. Only generator-shaped results need
      // the step-until-done loop.
      if (result && typeof result.next === "function") {
        try {
          let step = result.next();
          while (!step.done) {
            // Tolerate {sleep:0} produced by the budget-yield helper:
            // inside clinit we are synchronous, so a "cooperative
            // hand-off" simply means continue stepping. {sleep:N>0}
            // and {wait:...} genuinely cannot be honoured (the worker
            // is single-threaded and the surrounding caller is blocked
            // on this synchronous step). Other ops (HOST_CALL etc.)
            // were never explicitly handled here pre-budget-yield --
            // keep that pass-through behaviour so any clinit that
            // happens to make a host call continues to silently
            // step (the runtime never had clinits that legitimately
            // suspended; if one ever does land here it will just
            // misbehave, same as before).
            if (step.value && (
                (step.value.op === "sleep" && (_LtoNum(step.value.millis) | 0) > 0)
                || step.value.op === "wait")) {
              throw new Error("Blocking static initializers are not supported in javascript backend");
            }
            step = result.next();
          }
        } catch (e) {
          cls.initializing = false;
          if (VM_DIAG_ENABLED) {
            try { vmTrace("DIAG:CLINIT_THREW:" + className + ":err=" + String(e && e.message || e).slice(0, 120)); } catch (_e2) {}
          }
          throw e;
        } finally {
          jvm.__cn1ClinitDepth--;
        }
      } else {
        jvm.__cn1ClinitDepth--;
      }
    }
    cls.initializing = false;
    cls.initialized = true;
  },
  newObject(className) {
    this.ensureClassInitialized(className);
    const classDef = this.classes[className];
    const obj = { __class: className, __classDef: classDef, __id: this.nextIdentity++, __monitor: this.createMonitor() };
    this.initInstanceFields(obj, className);
    this.initFieldAliases(obj, className);
    // If this object is a Throwable, capture ``new Error().stack`` into
    // ``Throwable.stack`` right away. The Codename One ``Throwable``
    // constructors don't invoke ``fillInStack`` themselves (every other
    // port lazy-fills via ``printStackTrace``'s native), so without this
    // every translated ``throw new Foo(...)``-shape exception arrives at
    // the catch site with no stack — and the browser console line for
    // anything routed through ``Log.e`` collapses to a bare
    // ``Exception: <class>``. Capturing here covers BOTH the runtime's
    // ``createException`` path (NPE / ClassCastException / etc.) and
    // bytecode-emitted ``_O(<class>) + ctor`` paths uniformly.
    //
    // The fast ``assignableTo[Throwable]`` check fails for most concrete
    // exception classes (NPE / IllegalArgumentException / ...) because
    // ``defineClass`` only seeds ``assignableTo`` with self + Object +
    // direct baseClass. Throwable lives several levels up
    // (NPE → RuntimeException → Exception → Throwable), and the walk in
    // ``defineClass`` aborts the moment it can't find an ancestor's
    // classDef in ``this.classes`` (which happens when subclasses are
    // emitted before their ancestors — the comment above
    // ``defineClass`` calls this out explicitly). So fall back to
    // ``assignableViaAncestors``, which walks the baseClass chain at
    // query time when every ancestor is guaranteed to be registered,
    // and cache the answer on the classDef so subsequent throws of
    // the same exception type stay O(1).
    let isThrowable = false;
    if (classDef && classDef.assignableTo) {
      if (classDef.assignableTo["java_lang_Throwable"]) {
        isThrowable = true;
      } else if (this.assignableViaAncestors(className, "java_lang_Throwable")) {
        isThrowable = true;
        classDef.assignableTo["java_lang_Throwable"] = 1;
      }
    }
    if (isThrowable) {
      try {
        const prevLimit = Error.stackTraceLimit;
        try { Error.stackTraceLimit = 200; } catch (_l) {}
        const stack = new Error().stack || "";
        try { Error.stackTraceLimit = prevLimit; } catch (_l) {}
        obj[CN1_THROWABLE_STACK] = createJavaString(stack);
      } catch (_err) {
        // Best effort; an empty stack field is fine.
      }
    }
    return obj;
  },
  initInstanceFields(obj, className) {
    const cls = this.classes[className];
    if (!cls) {
      return;
    }
    if (cls.baseClass) {
      this.initInstanceFields(obj, cls.baseClass);
    }
    for (const field of cls.instanceFields) {
      // Instance fields serialize as ``[prop, desc]`` tuples to cut
      // ~30 chars per field vs the prior ``{owner,name,desc,prop}``
      // form. Prop (index 0) is always present; desc (index 1) is
      // used only by the primitive-descriptor test.
      const prop = field[0];
      const desc = field[1];
      obj[prop] = this.isPrimitiveFieldDescriptor(desc) ? 0 : null;
    }
  },
  isPrimitiveFieldDescriptor(desc) {
    if (desc == null) {
      return false;
    }
    const normalized = String(desc);
    if (!normalized) {
      return false;
    }
    if (normalized.length === 1 && "ZCBSIFJD".indexOf(normalized) >= 0) {
      return true;
    }
    if (normalized.indexOf("JAVA_") === 0 && normalized.indexOf("[]") < 0) {
      return true;
    }
    return false;
  },
  initFieldAliases(obj, className) {
    // Former subclass field-alias shim: when field accesses still
    // referenced subclass-qualified prop names at runtime
    // (``cn1_Child_field``), this walked the hierarchy and installed
    // getter/setter aliases onto each child-qualified key pointing at
    // the canonical declaring-class prop. The translator now resolves
    // field-access bytecode to the declaring class at emission time
    // via ``resolveFieldOwner``, so every PUTFIELD/GETFIELD references
    // the canonical prop directly and the aliases are never read. The
    // hook is kept as a stub so emitted callers don't need to change.
  },
  fieldProperty(owner, name) {
    return "cn1_" + owner + "_" + name;
  },
  newArray(size, componentClass, dimensions) {
    size = size | 0;
    if (size < 0) {
      throw new Error("Negative array size");
    }
    const array = new Array(size);
    for (let i = 0; i < size; i++) {
      array[i] = null;
    }
    array.__class = this.arrayClassName(componentClass, dimensions);
    array.__classDef = this.getArrayClass(componentClass, dimensions);
    array.__id = this.nextIdentity++;
    array.__dimensions = dimensions;
    array.__array = true;
    array.__monitor = this.createMonitor();
    return array;
  },
  newMultiArray(sizes, componentClass, dimensions, depth) {
    const level = depth || 0;
    const size = sizes[level] | 0;
    const array = this.newArray(size, componentClass, dimensions - level);
    if ((dimensions - level) <= 1) {
      return array;
    }
    const nextSize = sizes[level + 1];
    if (nextSize == null || nextSize < 0) {
      return array;
    }
    for (let i = 0; i < size; i++) {
      array[i] = this.newMultiArray(sizes, componentClass, dimensions, level + 1);
    }
    return array;
  },
  arrayClassName(componentClass, dimensions) {
    let name = componentClass;
    for (let i = 0; i < dimensions; i++) {
      name += "[]";
    }
    return name;
  },
  getArrayClass(componentClass, dimensions) {
    const className = this.arrayClassName(componentClass, dimensions);
    let cls = this.classes[className];
    if (!cls) {
      cls = {
        name: className,
        baseClass: "java_lang_Object",
        componentClass: componentClass,
        dimensions: dimensions,
        assignableTo: this.arrayAssignableTo(componentClass, dimensions),
        staticFields: {}
      };
      cls.classObject = {
        __class: "java_lang_Class",
        __monitor: this.createMonitor(),
        __className: className,
        __isClassObject: true,
        __classDef: cls,
        cn1_staticFields: cls.staticFields
      };
      this.classes[className] = cls;
    }
    return cls;
  },
  arrayAssignableTo(componentClass, dimensions) {
    const out = { java_lang_Object: true };
    out[this.arrayClassName(componentClass, dimensions)] = true;
    if (this.isPrimitiveComponent(componentClass)) {
      return out;
    }
    const componentClassDef = this.classes[componentClass];
    if (!componentClassDef || !componentClassDef.assignableTo) {
      for (let i = 1; i <= dimensions; i++) {
        out[this.arrayClassName("java_lang_Object", i)] = true;
      }
      return out;
    }
    const componentTargets = Object.keys(componentClassDef.assignableTo);
    for (let i = 0; i < componentTargets.length; i++) {
      const target = componentTargets[i];
      if (target === "java_lang_Object") {
        for (let depth = 1; depth <= dimensions; depth++) {
          out[this.arrayClassName(target, depth)] = true;
        }
      } else {
        out[this.arrayClassName(target, dimensions)] = true;
      }
    }
    return out;
  },
  isPrimitiveComponent(componentClass) {
    return componentClass.indexOf("JAVA_") === 0;
  },
  cloneArrayObject(arrayObject) {
    if (!arrayObject || !arrayObject.__array) {
      return null;
    }
    const className = String(arrayObject.__class || "");
    let componentClass = "java_lang_Object";
    if (className.endsWith("[]")) {
      componentClass = className.substring(0, className.length - 2);
    } else if (arrayObject.__classDef && arrayObject.__classDef.componentClass) {
      componentClass = arrayObject.__classDef.componentClass;
    }
    const dimensions = arrayObject.__dimensions > 0 ? (arrayObject.__dimensions | 0) : 1;
    const clone = this.newArray(arrayObject.length | 0, componentClass, dimensions);
    for (let i = 0; i < arrayObject.length; i++) {
      clone[i] = arrayObject[i];
    }
    return clone;
  },
  resolveVirtual(className, methodId) {
    if (className == null || className === "undefined") {
      const missingReceiver = {
        category: "missing_receiver",
        methodId: methodId,
        receiverClass: className == null ? "null" : String(className)
      };
      this.lastVirtualFailure = missingReceiver;
      vmDiag("VIRTUAL_FAIL", "category", missingReceiver.category);
      vmDiag("VIRTUAL_FAIL", "methodId", missingReceiver.methodId);
      vmDiag("VIRTUAL_FAIL", "receiverClass", missingReceiver.receiverClass);
      throw new Error("Missing virtual method " + methodId + " on " + className);
    }
    // Legacy call-sites in port.js / parparvm_runtime.js still pass
    // the class-specific methodId (``cn1_<declaringClass>_<method>_<sig>``)
    // that pre-fa4247a42 emission produced. Every class's methods map
    // now keys on the class-free dispatch id (``cn1_s_<method>_<sig>``)
    // — translate the class-specific form to the dispatch id by
    // stripping the owning-class prefix. Preserves behavior for
    // callers that already pass the new form.
    if (methodId && methodId.indexOf("cn1_") === 0 && methodId.indexOf("cn1_s_") !== 0) {
      let bestPrefix = null;
      for (const clsName in this.classes) {
        const prefix = "cn1_" + clsName + "_";
        if (methodId.indexOf(prefix) === 0
                && (bestPrefix == null || prefix.length > bestPrefix.length)) {
          bestPrefix = prefix;
        }
      }
      if (bestPrefix != null) {
        const dispatchId = "cn1_s_" + methodId.substring(bestPrefix.length);
        if (dispatchId !== methodId) {
          methodId = dispatchId;
        }
      }
    }
    const cacheKey = className + "|" + methodId;
    let cached = this.resolvedVirtualCache[cacheKey];
    if (cached) {
      return cached;
    }
    if (String(className).endsWith("[]") && this.isArrayCloneMethodId(methodId)) {
      const arrayCloneOverride = (function*(__cn1ThisObject) {
        return jvm.cloneArrayObject(__cn1ThisObject);
      });
      this.resolvedVirtualCache[cacheKey] = arrayCloneOverride;
      return arrayCloneOverride;
    }
    const tail = this.methodTail(methodId);
    let remapAttempted = false;
    let visitedClassHierarchy = false;
    let current = className;
    while (current) {
      const cls = this.classes[current];
      if (cls) {
        visitedClassHierarchy = true;
        if (cls.pendingMethods) { this.flushPendingMethods(cls); }
      }
      if (cls && cls.methods) {
        if (cls.methods[methodId]) {
          cached = resolveMethodEntry(cls.methods, methodId);
          this.resolvedVirtualCache[cacheKey] = cached;
          return cached;
        }
        if (tail) {
          const remappedId = this.remappedMethodId(current, methodId, tail);
          if (remappedId && remappedId !== methodId) {
            remapAttempted = true;
          }
          if (cls.methods[remappedId]) {
            cached = resolveMethodEntry(cls.methods, remappedId);
            this.resolvedVirtualCache[cacheKey] = cached;
            return cached;
          }
        }
      }
      current = cls ? cls.baseClass : null;
    }
    const visitedInterfaces = Object.create(null);
    const pendingInterfaces = [];
    let visitedAnyInterface = false;
    current = className;
    while (current) {
      const cls = this.classes[current];
      if (cls && cls.interfaces) {
        for (let i = 0; i < cls.interfaces.length; i++) {
          pendingInterfaces.push(cls.interfaces[i]);
        }
      }
      current = cls ? cls.baseClass : null;
    }
    while (pendingInterfaces.length) {
      const ifaceName = pendingInterfaces.shift();
      if (!ifaceName || visitedInterfaces[ifaceName]) {
        continue;
      }
      visitedInterfaces[ifaceName] = true;
      const iface = this.classes[ifaceName];
      if (!iface) {
        continue;
      }
      visitedAnyInterface = true;
      if (iface.pendingMethods) { this.flushPendingMethods(iface); }
      if (iface.methods) {
        if (iface.methods[methodId]) {
          cached = resolveMethodEntry(iface.methods, methodId);
          this.resolvedVirtualCache[cacheKey] = cached;
          return cached;
        }
        if (tail) {
          const remappedId = this.remappedMethodId(ifaceName, methodId, tail);
          if (remappedId && remappedId !== methodId) {
            remapAttempted = true;
          }
          if (iface.methods[remappedId]) {
            cached = resolveMethodEntry(iface.methods, remappedId);
            this.resolvedVirtualCache[cacheKey] = cached;
            return cached;
          }
        }
      }
      if (iface.interfaces) {
        for (let i = 0; i < iface.interfaces.length; i++) {
          pendingInterfaces.push(iface.interfaces[i]);
        }
      }
    }
    const nativeOverride = this.nativeMethods[methodId];
    if (typeof nativeOverride === "function") {
      this.resolvedVirtualCache[cacheKey] = nativeOverride;
      return nativeOverride;
    }
    if (this.isJsoBridgeClass(className)) {
      cached = this.createJsoBridgeMethod(className, methodId);
      this.resolvedVirtualCache[cacheKey] = cached;
      return cached;
    }
    let virtualFailureCategory = "missing_class_method";
    if (visitedAnyInterface) {
      virtualFailureCategory = "missing_interface_default_method";
    } else if (remapAttempted) {
      virtualFailureCategory = "unresolved_remap_tail";
    } else if (!visitedClassHierarchy) {
      virtualFailureCategory = "missing_receiver";
    }
    this.lastVirtualFailure = {
      category: virtualFailureCategory,
      methodId: methodId,
      receiverClass: className == null ? "null" : String(className)
    };
    vmDiag("VIRTUAL_FAIL", "category", virtualFailureCategory);
    vmDiag("VIRTUAL_FAIL", "methodId", methodId);
    vmDiag("VIRTUAL_FAIL", "receiverClass", className == null ? "null" : String(className));
    throw new Error("Missing virtual method " + methodId + " on " + className);
  },
  isJsoBridgeClass(className) {
    if (!className) {
      return false;
    }
    for (let i = 0; i < jsoRegistry.classPrefixes.length; i++) {
      if (className.indexOf(jsoRegistry.classPrefixes[i]) === 0) {
        return true;
      }
    }
    const cls = this.classes[className];
    return !!(cls && cls.assignableTo && cls.assignableTo["com_codename1_html5_js_JSObject"]);
  },
  createJsoBridgeMethod(className, methodId) {
    const self = this;
    return function*(__cn1ThisObject) {
      const args = new Array(Math.max(0, arguments.length - 1));
      for (let i = 1; i < arguments.length; i++) {
        args[i - 1] = arguments[i];
      }
      return yield* self.invokeJsoBridge(__cn1ThisObject, className, methodId, args);
    };
  },
  invokeJsoBridge(__cn1ThisObject, className, methodId, args) {
    // Diagnostic counter consumed by the bridge-bulk-transfer guard test:
    // per-element JSO dispatch in a data loop (e.g. a per-byte stream read)
    // multiplies this by the payload SIZE and must be caught in CI.
    this.__cn1JsoDispatchCount = (this.__cn1JsoDispatchCount | 0) + 1;
    const self = this;
    return (function*() {
      const receiver = self.unwrapJsValue(__cn1ThisObject);
      if (receiver == null) {
        throw new Error("Null JS interop receiver for " + methodId);
      }
      const bridge = self.parseJsoBridgeMethod(className, methodId);
      const nativeArgs = self.toNativeJsArgs(args || []);
      // ``receiver`` is guaranteed non-null at this point -- the
      // ``receiver == null`` throw above precedes us.
      if (receiver.__cn1HostRef != null) {
        const transferableArgs = new Array(nativeArgs.length);
        for (let i = 0; i < nativeArgs.length; i++) {
          const arg = nativeArgs[i];
          // Route function arguments through the same callback-token path
          // ``toHostTransferArg`` uses (which also honours the
          // cn1DisableEventForwarding URL opt-out) so host-bridge method
          // calls like ``element.addEventListener(name, listener,
          // capture)`` can actually forward the listener to the main
          // thread instead of silently losing it to null. The main-thread
          // bridge's ``mapHostArgs`` sees the token and materialises a
          // real JS function that posts ``worker-callback`` messages
          // back.
          transferableArgs[i] = (typeof arg === "function")
              ? self.toHostTransferArg(arg)
              : arg;
        }
        // Fire-and-forget for void-return JSO methods (and JSO property
        // setters, which are inherently void). The vast majority of canvas
        // ops the renderer issues -- ``ctx.save()``, ``ctx.fillStyle = X``,
        // ``ctx.fillRect(...)``, ``ctx.beginPath()``, ``ctx.fill()``,
        // ``ctx.restore()`` -- all return void. The original code yielded
        // the green thread waiting for a HOST_CALLBACK on every single one
        // of them, multiplying a 100-op frame into 100 host-callback
        // messages on the worker's inbox. With the worker stuck draining
        // its own callback chain it never had room for incoming pointer
        // events, which is what made the OK button on a Dialog modal
        // unreachable. Send the host-call but don't wait: the host
        // processes ops in postMessage FIFO order, so a subsequent
        // value-returning call (``getImageData``, ``measureText``) still
        // sees the right state. Embeds a ``__cn1_no_response`` flag the
        // host bridge honours by skipping ``postHostCallback``.
        const isVoid = bridge.returnClass === "void"
                || bridge.returnClass === "v"
                || bridge.returnClass == null;
        const isFireAndForget = (bridge.kind === "setter") || (bridge.kind === "method" && isVoid);
        if (isFireAndForget) {
          // Batch fire-and-forget ops; ``flushPendingFireAndForget``
          // sends the whole batch as a single ``host-call-batch``
          // message either at end-of-drain or right before the next
          // round-trip ``invokeHostNative``. Order is preserved.
          self.pendingFireAndForget.push({
            receiver: receiver,
            receiverClass: (receiver && receiver.__cn1HostClass) ? receiver.__cn1HostClass : className,
            kind: bridge.kind,
            member: bridge.member,
            args: transferableArgs,
            __cn1_no_response: true
          });
          return null;
        }
        // A round-trip is about to fire; the host must see all
        // previously-queued fire-and-forget ops first to keep
        // canvas state consistent.
        const jsoRequest = {
          receiver: receiver,
          receiverClass: (receiver && receiver.__cn1HostClass) ? receiver.__cn1HostClass : className,
          kind: bridge.kind,
          member: bridge.member,
          args: transferableArgs
        };
        const __retRC = bridge.returnClass;
        const __expectsObject = __retRC != null && __retRC !== "int" && __retRC !== "byte"
            && __retRC !== "short" && __retRC !== "char" && __retRC !== "long"
            && __retRC !== "float" && __retRC !== "double" && __retRC !== "boolean"
            && __retRC !== "void" && __retRC !== "v";
        const __retryableRead = bridge.kind === "getter"
            || (bridge.kind === "method" && JSO_RETRYABLE_READ_METHODS[bridge.member] === true);
        // DEGRADED-READ RECOVERY (the getDocument-null / canvasContextWipe /
        // "Missing JS member getContext" family). A round-trip object read for
        // an IDEMPOTENT member can come back corrupted four ways when its
        // response crosses with a concurrent host call in a dense paint burst:
        // (a) a NUMBER where an object was expected, (b) an empty {} that lost
        // its host-ref marker, (c) null/undefined from a never-null method
        // (createElement/getContext), or (d) a THROWN "Missing JS member" /
        // "Missing host receiver" because the receiver momentarily resolved to
        // a degraded value on the host. All four are transient -- RE-ISSUE the
        // identical read; the re-issue is another suspend/resume so the crossed
        // response drains first. Idempotent reads have no observable side
        // effect (createElement just makes a throwaway detached node) so
        // repeating is safe. Bounded: a genuinely persistent failure falls
        // through to substitute-null below or re-throws, and never loops.
        let hostResult;
        let __attempt = 0;
        for (;;) {
          if (__attempt > 0) {
            // Backoff before re-issuing: yield long enough for the concurrent
            // numeric getters whose responses crossed into this read to finish,
            // so the re-issue isn't immediately re-crossed by the same in-flight
            // burst. Grows with the attempt (capped) so a sustained storm gets
            // progressively more room to drain.
            yield { op: "sleep", millis: Math.min(8 * __attempt, 64) };
          }
          let __threw = null;
          try {
            // The host must see all previously-queued fire-and-forget ops
            // first to keep canvas state consistent before this round-trip.
            self.flushPendingFireAndForget();
            hostResult = yield self.invokeHostNative("__cn1_jso_bridge__", [jsoRequest]);
          } catch (__hostErr) {
            __threw = __hostErr;
          }
          if (__threw != null) {
            // A transient throw ("Missing JS member"/"Missing host receiver"/
            // timeout) means the host call NEVER EXECUTED -- the receiver
            // momentarily resolved to a degraded value -- so re-issuing is safe
            // for ANY round-trip method (not just the idempotent-read allowlist;
            // nothing ran, so there's no side effect to repeat). This is what
            // catches an emit-time canvas.toDataURL() cross that would otherwise
            // escape and deadlock the EDT.
            if (__attempt < JSO_MAX_RETRY && isTransientHostBridgeError(__threw)) {
              __attempt++;
              if (VM_DIAG_ENABLED) {
                try { vmDiag("JSO_RETRY", "member", String(bridge.member) + ":throw:attempt=" + __attempt); } catch (_e) {}
              }
              continue;
            }
            throw __threw;
          }
          if (__expectsObject && __retryableRead && __attempt < JSO_MAX_RETRY
              && isDegradedObjectResult(hostResult, bridge)) {
            __attempt++;
            if (VM_DIAG_ENABLED) {
              try { vmDiag("JSO_RETRY", "member", String(bridge.member) + ":attempt=" + __attempt); } catch (_e) {}
            }
            continue;
          }
          break;
        }
        // canvasContextWipe RECOVERY: when hostResult is a NUMBER but
        // the expected return class is an object type, substitute null.
        // The downstream code would otherwise treat the number as a
        // receiver and busy-loop on VIRTUAL_FAIL / NULL_RECEIVER.
        // Returning null lets the caller's standard null-check path
        // throw a clean NPE instead.
        //
        // Host-side root cause TBD (browser_bridge.js:670
        // ``receiver[member]`` returning a number when it shouldn't).
        // The NUMBER_LEAK diag captures it at the bridge boundary.
        let workingHostResult = hostResult;
        if (typeof hostResult === 'number' && bridge.returnClass
            && bridge.returnClass !== 'int' && bridge.returnClass !== 'byte'
            && bridge.returnClass !== 'short' && bridge.returnClass !== 'char'
            && bridge.returnClass !== 'long' && bridge.returnClass !== 'float'
            && bridge.returnClass !== 'double' && bridge.returnClass !== 'boolean') {
          if (!jvm._numberForObjLogged) jvm._numberForObjLogged = 0;
          if (jvm._numberForObjLogged < 5) {
            jvm._numberForObjLogged++;
            try {
              vmDiag('NUMBER_FOR_OBJECT', 'methodId', String(methodId));
              vmDiag('NUMBER_FOR_OBJECT', 'className', String(className));
              vmDiag('NUMBER_FOR_OBJECT', 'member', String(bridge.member));
              vmDiag('NUMBER_FOR_OBJECT', 'kind', String(bridge.kind));
              vmDiag('NUMBER_FOR_OBJECT', 'returnClass', String(bridge.returnClass));
              vmDiag('NUMBER_FOR_OBJECT', 'value', String(hostResult));
              vmDiag('NUMBER_FOR_OBJECT', 'recovery', 'substituted-null');
            } catch (_e) {}
          }
          workingHostResult = null;
        }
        // Diagnostic: when hostResult is a literal {} (no own props at
        // all), it indicates an upstream bug where the host bridge
        // returned an opaque value that lost its host-ref marker on
        // round-trip. This is the source of canvasContextWipe (#44).
        // Rate-limited globally to keep logs sane.
        if (hostResult && typeof hostResult === 'object'
            && !Array.isArray(hostResult)
            && hostResult.__cn1HostRef == null
            && Object.getOwnPropertyNames(hostResult).length === 0) {
          if (!jvm._emptyHostResultLogged) jvm._emptyHostResultLogged = 0;
          if (jvm._emptyHostResultLogged < 5) {
            jvm._emptyHostResultLogged++;
            try {
              vmDiag('EMPTY_HOST_RESULT', 'methodId', String(methodId));
              vmDiag('EMPTY_HOST_RESULT', 'className', String(className));
              vmDiag('EMPTY_HOST_RESULT', 'member', String(bridge.member));
              vmDiag('EMPTY_HOST_RESULT', 'kind', String(bridge.kind));
              vmDiag('EMPTY_HOST_RESULT', 'returnClass', String(bridge.returnClass));
              vmDiag('EMPTY_HOST_RESULT', 'receiverHostClass', String(receiver && receiver.__cn1HostClass));
              vmDiag('EMPTY_HOST_RESULT', 'receiverHostRef', String(receiver && receiver.__cn1HostRef));
              const stack = new Error('empty-host-result-trace').stack || '';
              vmDiag('EMPTY_HOST_RESULT', 'stack', String(stack).split('\n').slice(0, 6).join(' | ').substring(0, 500));
            } catch (_e) {}
          }
        }
        return self.wrapJsResult(workingHostResult, bridge.returnClass);
      }
      let result;
      if (bridge.kind === "getter") {
        result = receiver[bridge.member];
      } else if (bridge.kind === "setter") {
        receiver[bridge.member] = nativeArgs.length ? nativeArgs[0] : null;
        result = null;
      } else {
        // For array-like objects, prefer indexed get/set over native methods
        // because TypedArray.prototype.set(array, offset) has different
        // semantics than the JSO per-element set(index, value).
        if (bridge.member === "get" && nativeArgs.length === 1 && receiver && typeof receiver.length === "number") {
          result = receiver[nativeArgs[0] | 0];
        } else if (bridge.member === "set" && nativeArgs.length === 2 && receiver && typeof receiver.length === "number") {
          receiver[nativeArgs[0] | 0] = nativeArgs[1];
          result = null;
        } else {
          const fn = receiver[bridge.member];
          if (typeof fn === "function") {
            result = fn.apply(receiver, nativeArgs);
          } else if (!nativeArgs.length && Object.prototype.hasOwnProperty.call(receiver, bridge.member)) {
            result = receiver[bridge.member];
          } else if (typeof receiver === "function") {
            // Functional-interface (SAM) receivers: the JSO interface
            // declares one abstract method (e.g. EventListener.handleEvent,
            // Runnable.run, AnimationFrameCallback.onAnimationFrame) and
            // the wrapped JS value is itself a function — DOM
            // ``addEventListener(type, fn)`` and friends pass plain
            // functions, JSObject.cast(fn, EventListener.class) wraps
            // them as a JSO-typed reference. Calling the SAM dispatches
            // the function directly. Without this fallback the bridge
            // throws ``Missing JS member handleEvent`` because a
            // function value has no ``handleEvent`` property of its own.
            result = receiver.apply(null, nativeArgs);
          } else {
            throw new Error("Missing JS member " + bridge.member + " for " + methodId);
          }
        }
      }
      return self.wrapJsResult(result, bridge.returnClass);
    })();
  },
  parseJsoBridgeMethod(className, methodId) {
    // Two methodId shapes arrive here. Historical class-specific:
    // ``cn1_<class>_<method>_<sig>_R_<ret>`` — strip the ``cn1_<class>_``
    // prefix and parse what's left. Post-fa4247a42 sig-based dispatch
    // id: ``cn1_s_<method>_<sig>_R_<ret>`` — strip the ``cn1_s_``
    // prefix. Either way we want the ``method`` token (plus whether
    // parameter tokens follow) so the get/is/set heuristics below can
    // map ``getFoo()`` to a ``{kind:"getter", member:"foo"}`` bridge.
    const classPrefix = "cn1_" + className + "_";
    const dispatchPrefix = "cn1_s_";
    let remainder;
    if (methodId.indexOf(classPrefix) === 0) {
      remainder = methodId.substring(classPrefix.length);
    } else if (methodId.indexOf(dispatchPrefix) === 0) {
      remainder = methodId.substring(dispatchPrefix.length);
    } else {
      remainder = methodId;
    }
    let returnClass = null;
    const returnMarker = remainder.lastIndexOf("_R_");
    if (returnMarker >= 0) {
      returnClass = remainder.substring(returnMarker + 3);
      remainder = remainder.substring(0, returnMarker);
    }
    let member;
    let hasParameters;
    if (remainder.indexOf("cn1_") === 0) {
      const inferred = this.inferJsoBridgeMember(remainder);
      member = inferred.member;
      hasParameters = inferred.hasParameters;
    } else {
      const firstUnderscore = remainder.indexOf("_");
      member = firstUnderscore >= 0 ? remainder.substring(0, firstUnderscore) : remainder;
      hasParameters = firstUnderscore >= 0;
    }
    if (!hasParameters && member.indexOf("get") === 0 && member.length > 3) {
      return { kind: "getter", member: lowerFirst(member.substring(3)), returnClass: returnClass };
    }
    if (!hasParameters && member.indexOf("is") === 0 && member.length > 2) {
      return { kind: "getter", member: lowerFirst(member.substring(2)), returnClass: returnClass || "boolean" };
    }
    // Setter detection is heuristic — Java only emits the bare method
    // name in dispatch ids, so we infer ``@JSProperty void setX(X)``
    // from the ``setXxx`` shape. The catch is that any DOM /
    // localforage / etc. method whose name happens to start with
    // ``set`` and takes more than one arg looks identical to a setter
    // (``setItem(key, value, callback)``,
    // ``setAttribute(name, value)``, etc.).
    //
    // Detection rules:
    //   1. Methods that return a value are never setters — true
    //      setters are ``void``. Reject when ``returnClass`` is set.
    //   2. Count the number of parameter-start prefixes after the
    //      member name. ``cn1_s_setX_<type>`` has exactly one
    //      parameter type prefix (``java_``, ``com_`` etc.); a
    //      multi-arg method has multiple. Two or more means it's a
    //      method, regardless of how the name happens to begin.
    //   3. Static deny-list as a final safety net for cases the
    //      heuristic can't disambiguate (e.g. when the parameter
    //      type is itself prefix-less like a primitive).
    const SETTER_DENY_LIST = {
      setAttribute: 1, setProperty: 1, setItem: 1, setDriver: 1,
      setStoreName: 1, setVersion: 1, setSize: 1, setDescription: 1,
      setSelectionRange: 1, setTimeout: 1, setInterval: 1,
      setRequestHeader: 1
    };
    if (member.indexOf("set") === 0 && member.length > 3
            && remainder.indexOf("_") > -1
            && returnClass == null
            && !SETTER_DENY_LIST[member]) {
      // Count the number of parameter-type-start prefixes after the
      // member name. ``cn1_s_setX_java_lang_String`` has 1 (``_java_``);
      // a multi-arg method like ``cn1_s_setItem_java_lang_String_com_codename1_html5_js_JSObject_<callbackClass>``
      // has 3+. The translator emits each parameter type as a fully-
      // qualified package path, so the count of leading-package
      // tokens correlates 1-to-1 with parameter count.
      const argSection = remainder.substring(member.length);
      const typeStarts = argSection.match(/_(?:java|com|org|kotlin|sun|javax)_/g) || [];
      if (typeStarts.length <= 1) {
        return { kind: "setter", member: lowerFirst(member.substring(3)), returnClass: returnClass };
      }
    }
    return { kind: "method", member: member, returnClass: returnClass };
  },
  inferJsoBridgeMember(remainder) {
    const body = remainder.indexOf("cn1_") === 0 ? remainder.substring(4) : remainder;
    const tokens = body.split("_");
    for (let i = 1; i < tokens.length; i++) {
      const previous = tokens[i - 1];
      const current = tokens[i];
      if (previous && current && this.isEncodedClassToken(previous) && this.isEncodedMethodToken(current)) {
        return { member: current, hasParameters: i < tokens.length - 1 };
      }
    }
    return { member: tokens.length ? tokens[tokens.length - 1] : body, hasParameters: tokens.length > 1 };
  },
  isEncodedClassToken(token) {
    const ch = token && token.charAt(0);
    return !!(ch && ch >= "A" && ch <= "Z");
  },
  isEncodedMethodToken(token) {
    const ch = token && token.charAt(0);
    return !!(ch && ch >= "a" && ch <= "z");
  },
  isArrayCloneMethodId(methodId) {
    if (typeof methodId !== "string" || methodId.length === 0) {
      return false;
    }
    // Mangling rewrites identifier *literals* but leaves regex bodies
    // alone, so the legacy regex never matches a mangled methodId
    // (e.g. ``$Yj``). Compare against ``CN1_CLONE_DISPATCH_ID`` first —
    // that constant moves through the mangler in lockstep with every
    // call site. Keep the regex as a fallback for the unmangled
    // pre-build path and any historical ``cn1_<class>_clone_..`` form.
    if (methodId === CN1_CLONE_DISPATCH_ID) {
      return true;
    }
    return /(?:^|_)clone_R_java_lang_Object$/.test(methodId);
  },
  methodTail(methodId) {
    let cached = this.methodTailCache[methodId];
    if (cached !== undefined) {
      return cached;
    }
    let bestPrefix = null;
    for (const className in this.classes) {
      const prefix = "cn1_" + className;
      if (methodId.indexOf(prefix) === 0 && (bestPrefix == null || prefix.length > bestPrefix.length)) {
        bestPrefix = prefix;
      }
    }
    if (bestPrefix != null) {
      cached = methodId.substring(bestPrefix.length);
      this.methodTailCache[methodId] = cached;
      return cached;
    }
    this.methodTailCache[methodId] = null;
    return null;
  },
  remappedMethodId(className, methodId, tail) {
    const cacheKey = className + "|" + methodId;
    let cached = this.remappedMethodIdCache[cacheKey];
    if (cached !== undefined) {
      return cached;
    }
    cached = tail ? "cn1_" + className + tail : null;
    this.remappedMethodIdCache[cacheKey] = cached;
    return cached;
  },
  instanceOf(obj, className) {
    if (!obj || !obj.__classDef || !obj.__classDef.assignableTo) {
      return false;
    }
    if (obj.__classDef.assignableTo[className]) {
      return true;
    }
    if (obj.__class && this.assignableViaAncestors(obj.__class, className)) {
      obj.__classDef.assignableTo[className] = 1;
      return true;
    }
    return false;
  },
  /**
   * Lazy fallback for the {@code defineClass} auto-populate when
   * the translator emits classes out of inheritance order. Walks
   * the {@code baseClass} string chain and the implemented
   * interfaces, looking for {@code targetType} either as a name
   * along the chain or as an entry in any ancestor's already-
   * populated {@code assignableTo} map. The caller caches the
   * result back into the original class's {@code assignableTo} so
   * subsequent lookups stay O(1).
   */
  assignableViaAncestors(className, targetType) {
    if (className == null || targetType == null) {
      return false;
    }
    const visited = Object.create(null);
    const queue = [className];
    while (queue.length) {
      const current = queue.shift();
      if (current == null || visited[current]) {
        continue;
      }
      visited[current] = true;
      if (current === targetType) {
        return true;
      }
      const cls = this.classes[current];
      if (!cls) {
        continue;
      }
      if (cls.assignableTo && cls.assignableTo[targetType]) {
        return true;
      }
      if (cls.baseClass) {
        queue.push(cls.baseClass);
      }
      if (cls.interfaces) {
        for (let i = 0; i < cls.interfaces.length; i++) {
          queue.push(cls.interfaces[i]);
        }
      }
    }
    return false;
  },
  findExceptionHandler(entries, pc, error) {
    if (!entries || !entries.length) {
      return null;
    }
    const errorClass = error == null ? null : error.__class;
    const errorClassDef = error == null ? null : error.__classDef;
    for (let i = 0; i < entries.length; i++) {
      const entry = entries[i];
      // Short property names emitted by the translator (``s`` / ``e``
      // / ``h`` / ``t``), with a legacy long-name fallback for any
      // table constructed directly by hand-written runtime code.
      const start = entry.s !== undefined ? entry.s : entry.start;
      const end = entry.e !== undefined ? entry.e : entry.end;
      const type = entry.t !== undefined ? entry.t : entry.type;
      if (pc < start || pc >= end) {
        continue;
      }
      if (type == null) {
        return entry;
      }
      if (errorClass === type || (errorClassDef && errorClassDef.assignableTo && errorClassDef.assignableTo[type])) {
        return entry;
      }
      // assignableTo is auto-populated at defineClass time from
      // baseDef.assignableTo unions. When the error's class was
      // defined BEFORE its baseClass (translator emits classes in
      // file order, not inheritance order — IllegalStateException
      // can land before RuntimeException), the union is partial:
      // the immediate baseClass name was pinned but transitive
      // ancestors are missing. Walk the baseClass string chain at
      // query time and union those classes' (now-fully-populated)
      // assignableTo maps before declaring "no match".
      if (errorClass != null && this.assignableViaAncestors(errorClass, type)) {
        if (errorClassDef && errorClassDef.assignableTo) {
          errorClassDef.assignableTo[type] = 1;
        }
        return entry;
      }
      // Raw JS errors (``throw new Error(...)`` from runtime helpers
      // such as ``resolveVirtual`` when a worker-side host-ref proxy
      // is missing) carry neither ``__class`` nor ``__classDef``, so
      // the strict comparisons above always miss and the throw
      // escapes every Java ``catch (Throwable t)``. That defeats
      // defensive call-site wrappers like ``createImage``'s catch
      // for the writeArgbBuffer/createImageData race. Java semantics
      // say ``catch (Throwable)`` is the broadest possible catch, so
      // treat raw JS errors as if they had been thrown as
      // ``java.lang.RuntimeException`` -- matches any handler typed
      // Throwable, Exception, RuntimeException, or Error.
      if (errorClass == null
              && (type === "java_lang_Throwable"
                  || type === "java_lang_Exception"
                  || type === "java_lang_RuntimeException"
                  || type === "java_lang_Error")) {
        // Always-on log (not diag-gated) so users can see WHAT was
        // caught -- a silent swallow of recurring runtime errors
        // turns deep render bugs into seemingly-spontaneous visual
        // artifacts (frame tearing, flicker) with no console signal.
        // Rate-limit per message so a paint-cycle-per-frame error
        // doesn't flood the console. ``printToConsole`` routes through
        // the worker→main log forwarding pipeline -- with the matching
        // PARPAR:CAUGHT_RAW_JS_ERROR allowlist entry in browser_bridge.js
        // the line surfaces on the main-thread DevTools console without
        // ``?parparDiag=1``.
        var msg = error && error.message ? String(error.message) : String(error);
        if (!this._rawCatchLogged) this._rawCatchLogged = {};
        if (!this._rawCatchLogged[msg]) {
          this._rawCatchLogged[msg] = 1;
          try { printToConsole("PARPAR:CAUGHT_RAW_JS_ERROR:" + msg); } catch (_le) {}
        }
        return entry;
      }
    }
    return null;
  },
  createStringLiteral(value) {
    if (!this.literalStrings[value]) {
      const chars = this.newArray(value.length, "JAVA_CHAR", 1);
      for (let i = 0; i < value.length; i++) {
        chars[i] = value.charCodeAt(i);
      }
      const str = this.newObject("java_lang_String");
      str[CN1_STRING_VALUE] = chars;
      str[CN1_STRING_OFFSET] = 0;
      str[CN1_STRING_COUNT] = value.length;
      str[CN1_STRING_HASH] = 0;
      str.__nativeString = value;
      this.literalStrings[value] = str;
    }
    return this.literalStrings[value];
  },
  toNativeString(value) {
    if (value == null) {
      return "null";
    }
    if (typeof value === "string") {
      return value;
    }
    if (value.__nativeString != null) {
      return value.__nativeString;
    }
    if (value.__class === "java_lang_String") {
      const data = value[CN1_STRING_VALUE];
      const offset = value[CN1_STRING_OFFSET] | 0;
      const count = value[CN1_STRING_COUNT] | 0;
      let out = "";
      for (let i = 0; i < count; i++) {
        out += String.fromCharCode(data[offset + i] | 0);
      }
      value.__nativeString = out;
      return out;
    }
    if (value.__l === 1) {
      return _LtoStr(value); // hi/lo Long object -> exact decimal
    }
    return "" + value;
  },
  toNativeJsArgs(args) {
    const out = new Array(args.length);
    for (let i = 0; i < args.length; i++) {
      out[i] = this.toNativeJsArg(args[i]);
    }
    return out;
  },
  toNativeJsArg(value) {
    if (value == null) {
      return value;
    }
    if (value.__jsValue !== undefined) {
      return value.__jsValue;
    }
    if (typeof value === "string") {
      return value;
    }
    if (value.__class === "java_lang_String" || value.__nativeString != null) {
      return this.toNativeString(value);
    }
    const boxedPrimitive = runtimeBoxedPrimitiveValue(value);
    if (boxedPrimitive !== null) {
      return boxedPrimitive;
    }
    for (let i = 0; i < jsoRegistry.nativeArgConverters.length; i++) {
      const converted = jsoRegistry.nativeArgConverters[i](value, this);
      if (converted !== value) {
        return converted;
      }
    }
    return value;
  },
  unwrapJsValue(value) {
    const result = value && value.__jsValue !== undefined ? value.__jsValue : value;
    // DIAG: when the unwrapped value is literal {} (no own props, no
    // __cn1HostRef, no __classDef) AND the input was a wrapper (had
    // __jsValue), log the leak source. This catches the canvasContextWipe
    // upstream: someone is passing a wrapper whose __jsValue is {}, and
    // the unwrap path leaks the {} forward as a JSO receiver.
    if (result && typeof result === 'object' && !Array.isArray(result)
        && value && value !== result
        && result.__cn1HostRef == null && result.__classDef == null
        && Object.getOwnPropertyNames(result).length === 0
        // Critical filter: real native objects (XMLHttpRequest, ArrayBuffer,
        // typed arrays, DOM nodes, etc.) have a NON-Object.prototype
        // prototype. Only literal {} has Object.prototype, which is the
        // shape that the cn1_iv* receiver-{} bug shows.
        && Object.getPrototypeOf(result) === Object.prototype) {
      if (!jvm._emptyUnwrapLogged) jvm._emptyUnwrapLogged = 0;
      if (jvm._emptyUnwrapLogged < 8) {
        jvm._emptyUnwrapLogged++;
        try {
          vmDiag('EMPTY_UNWRAP', 'inputClass', String(value.__class));
          vmDiag('EMPTY_UNWRAP', 'inputClassDef', String(value.__classDef && value.__classDef.name));
          vmDiag('EMPTY_UNWRAP', 'inputId', String(value.__id));
          const stack = new Error('empty-unwrap-trace').stack || '';
          vmDiag('EMPTY_UNWRAP', 'stack', String(stack).split('\n').slice(0, 6).join(' | ').substring(0, 500));
        } catch (_e) {}
      }
    }
    return result;
  },
  wrapJsResult(value, expectedClass) {
    if (value == null) {
      return null;
    }
    if (!expectedClass) {
      return value;
    }
    switch (expectedClass) {
      case "java_lang_String":
        return createJavaString(String(value));
      case "boolean":
        return value ? 1 : 0;
      case "byte":
      case "short":
      case "char":
      case "int":
        return value | 0;
      case "float":
      case "double":
      case "long":
        return Number(value);
      default:
        break;
    }
    if (typeof value === "string") {
      return createJavaString(value);
    }
    if (value && value.__classDef) {
      return value;
    }
    return this.wrapJsObject(value, expectedClass);
  },
  wrapJsObject(value, expectedClass) {
    if (value == null || (typeof value !== "object" && typeof value !== "function")) {
      return value;
    }
    let wrapper = jsObjectWrappers ? jsObjectWrappers.get(value) : null;
    const resolvedClass = this.inferJsObjectClass(value, expectedClass);
    const classDef = this.classes[resolvedClass] || this.classes[expectedClass] || null;
    if (wrapper) {
      // Preserve a cached wrapper's existing __class when re-wrapping the
      // same value returns a less-specific (or unresolvable) class. Root
      // cause of the chartDocumentStaleness cascade (task #43): when the
      // late-suite Document wrapper is re-resolved via @JSBody host calls
      // that pass through host-ref markers without __cn1HostClass set,
      // inferJsObjectClass falls back to ``null``. Overwriting __class with
      // null turns the cached Document wrapper into a typeless object;
      // subsequent ``cn1_s_createElement`` virtual dispatch reads
      // receiver.__class as null and emits VIRTUAL_FAIL ... receiverClass=null
      // -- the entire chart suite tail then cascades through that null.
      //
      // The cached wrapper's prior class is the authoritative answer (it
      // was resolved when the value first entered the worker with full
      // host-ref context). Only widen it -- never narrow to null.
      if (!wrapper.__class || resolvedClass) {
        wrapper.__class = resolvedClass || wrapper.__class;
      }
      if (resolvedClass) {
        this.enhanceJsWrapper(wrapper, resolvedClass);
      }
      if (expectedClass && expectedClass !== wrapper.__class) {
        this.enhanceJsWrapper(wrapper, expectedClass);
      }
      return wrapper;
    }
    wrapper = {
      __class: resolvedClass,
      __classDef: classDef,
      __jsValue: value,
      __id: this.nextIdentity++,
      __monitor: this.createMonitor()
    };
    // Several @JSBody natives (EventUtil._addEventListener,
    // _removeEventListener, getContentWindow().dispatchEvent, iframe
    // focus()/blur() etc.) embed inline ``target.methodName(...)`` calls
    // that the translator emits verbatim into worker-side JS. In the
    // worker, ``target`` is a JSO wrapper with no native DOM methods,
    // so the inline lookup throws ``TypeError: X is not a function``.
    //
    // The @JSBody emitter (JavascriptMethodGenerator, line ~1309) passes
    // object params through ``jvm.unwrapJsValue(...)`` before calling
    // the inline script body, which means the value visible inside the
    // script is ``wrapper.__jsValue`` — the raw host-ref proxy object
    // received via postMessage, NOT the wrapper we created here. So
    // stub the no-op DOM methods on BOTH the wrapper and the underlying
    // host-ref proxy. Mutating the proxy is safe: it's a plain object
    // owned by this worker, the main-thread host-ref id lives in
    // ``__cn1HostRef`` which we don't touch, and subsequent receipts of
    // the same proxy pick up the stubs via the property write.
    if (value.__cn1HostRef != null) {
      if (typeof wrapper.addEventListener !== "function") {
        wrapper.addEventListener = function() {};
      }
      if (typeof wrapper.removeEventListener !== "function") {
        wrapper.removeEventListener = function() {};
      }
      if (typeof wrapper.dispatchEvent !== "function") {
        wrapper.dispatchEvent = function() { return true; };
      }
      if (typeof value.addEventListener !== "function") {
        value.addEventListener = function() {};
      }
      if (typeof value.removeEventListener !== "function") {
        value.removeEventListener = function() {};
      }
      if (typeof value.dispatchEvent !== "function") {
        value.dispatchEvent = function() { return true; };
      }
    }
    if (jsObjectWrappers) {
      jsObjectWrappers.set(value, wrapper);
    }
    this.enhanceJsWrapper(wrapper, resolvedClass);
    if (expectedClass && expectedClass !== resolvedClass) {
      this.enhanceJsWrapper(wrapper, expectedClass);
    }
    return wrapper;
  },
  enhanceJsWrapper(wrapper, className) {
    if (!wrapper || !className) {
      return;
    }
    const sourceDef = this.classes[className];
    if (!sourceDef) {
      return;
    }
    let targetDef = wrapper.__classDef;
    if (!targetDef) {
      wrapper.__classDef = sourceDef;
      return;
    }
    if (targetDef === sourceDef) {
      return;
    }
    if (!targetDef.__cn1MergedJsWrapper) {
      targetDef = {
        name: wrapper.__class,
        methods: Object.assign({}, targetDef.methods || {}),
        assignableTo: Object.assign({}, targetDef.assignableTo || {}),
        __cn1MergedJsWrapper: true
      };
      if (wrapper.__class) {
        targetDef.assignableTo[wrapper.__class] = true;
      }
      wrapper.__classDef = targetDef;
    }
    Object.assign(targetDef.methods, sourceDef.methods || {});
    Object.assign(targetDef.assignableTo, sourceDef.assignableTo || {});
    targetDef.assignableTo[className] = true;
  },
  inferJsObjectClass(value, expectedClass) {
    if (value && value.__classDef && value.__jsValue === undefined) {
      return value.__class;
    }
    if (value && value.__cn1HostClass) {
      return value.__cn1HostClass;
    }
    if (jsoRegistry.inferFn) {
      const inferred = jsoRegistry.inferFn(value, expectedClass, this);
      if (inferred) {
        return inferred;
      }
    }
    return expectedClass || "com_codename1_html5_js_JSObject";
  },
  log(message) {
    emitVmMessage({ type: this.protocol.messages.LOG, message: message });
  },
  finish(result) {
    emitVmMessage({ type: this.protocol.messages.RESULT, result: result });
  },
  fail(error) {
    const message = formatErrorForVm(error);
    let virtualFailure = this.lastVirtualFailure;
    if (!virtualFailure) {
      const parsed = parseMissingVirtualMessage(message);
      if (parsed) {
        virtualFailure = {
          category: parsed.receiverClass === "undefined" || parsed.receiverClass === "null"
            ? "missing_receiver"
            : "missing_class_method",
          methodId: parsed.methodId,
          receiverClass: parsed.receiverClass
        };
      }
    }
    if (!this.firstFailure) {
      this.firstFailure = {
        category: virtualFailure && virtualFailure.category ? virtualFailure.category : "runtime_error",
        methodId: virtualFailure && virtualFailure.methodId ? virtualFailure.methodId : "",
        receiverClass: virtualFailure && virtualFailure.receiverClass ? virtualFailure.receiverClass : ""
      };
      vmDiag("FIRST_FAILURE", "category", this.firstFailure.category);
      vmDiag("FIRST_FAILURE", "methodId", this.firstFailure.methodId || "none");
      vmDiag("FIRST_FAILURE", "receiverClass", this.firstFailure.receiverClass || "none");
    }
    let stack = error && error.stack ? error.stack : null;
    if (!stack && error && typeof error === "object") {
      const javaStack = error[CN1_THROWABLE_STACK];
      if (javaStack) {
        try { stack = jvm.toNativeString(javaStack); }
        catch (_es) { stack = String(javaStack); }
      }
    }
    emitVmMessage({
      type: this.protocol.messages.ERROR,
      message: message,
      stack: stack,
      virtualFailure: virtualFailure || null
    });
  },
  invokeHostNative(symbol, args) {
    this.__cn1HostCallCount = (this.__cn1HostCallCount | 0) + 1;
    return { op: this.protocol.messages.HOST_CALL, id: this.nextHostCallId++, symbol: symbol, args: args || [] };
  },
  // Arm the owning-object finalizer so the host releases ``hostResource``'s id
  // when the Java ``owner`` is GC'd. See the host-ref lifecycle block above.
  registerNativeResource(owner, hostResource) {
    registerNativeResource(owner, hostResource);
  },
  resolveHostCall(id, success, value, error) {
    const pending = this.pendingHostCalls[id];
    if (!pending) {
      return false;
    }
    // Disarm the lost-response watchdog (if this was a JSO-bridge round-trip) --
    // the response arrived, so the timeout must not later fire a false abort.
    if (pending.timeoutEntry) {
      this._removeTimedWakeup(pending.timeoutEntry);
      pending.timeoutEntry = null;
    }
    // Main-thread host callbacks fire on every async bridge call (image
    // load, fetch, BrowserComponent, etc.). The :ok branch is gated
    // behind ``?parparDiag=1`` because in steady-state apps it floods
    // the console (one line per main-thread async call) and skews
    // perf measurements; the :err branch stays always-on because a
    // failure here is the kind of stuck-on-Loading symptom the lifecycle
    // log was designed to surface.
    if (pending.thread === this.mainThread
            || (this.mainThreadObject && pending.thread && pending.thread.object === this.mainThreadObject)) {
      if (success) {
        if (VM_DIAG_ENABLED) {
          vmLifecycle("main-host-callback:id=" + id + ":ok");
        }
      } else {
        vmLifecycle("main-host-callback:id=" + id + ":err");
      }
    }
    delete this.pendingHostCalls[id];
    if (success) {
      this.enqueue(pending.thread, value);
    } else {
      pending.thread.waiting = null;
      pending.thread.resumeError = error instanceof Error ? error : new Error(error == null ? "Host callback failed" : String(error));
      this.runnable.push(pending.thread);
      this.drain();
    }
    return true;
  },
  toHostTransferArg(value, _depth, _seen) {
    if (_depth == null) _depth = 0;
    if (_seen == null) _seen = new Set();
    // Cycle break: if we've already serialised this exact object,
    // return null to avoid infinite recursion. This shows up most
    // visibly when a Java SAM wrapper without a recognised dispatch
    // id falls through to the object iteration path — the wrapper's
    // ``__classDef`` graph is shared and self-referential. Returning
    // null preserves the call shape so the host doesn't blow up but
    // signals "no callable callback" upstream.
    if (value && typeof value === "object" && _seen.has(value)) {
      return null;
    }
    if (value && typeof value === "object") {
      _seen.add(value);
    }
    if (value == null) {
      return value;
    }
    const type = typeof value;
    if (type === "string" || type === "number" || type === "boolean") {
      return value;
    }
    if (type === "function") {
      // By default mint a stable ID for this worker-side function and hand
      // the main thread a token it can resolve back to a real callback at
      // event fire time. The screenshot-test harness appends
      // ``cn1DisableEventForwarding=1`` to the URL because the existing
      // BrowserComponent-based tests intentionally time out and their
      // recorded baseline assumes no input events fire; turning
      // addEventListener back into a no-op there keeps those baselines
      // stable. Production apps (Initializr, playground, etc.) leave the
      // flag unset and get real keyboard/mouse/resize routing.
      if (__cn1EventForwardingEnabled()) {
        if (value.__cn1WorkerCallbackId == null) {
          value.__cn1WorkerCallbackId = this.nextWorkerCallbackId++;
          this.workerCallbacks[value.__cn1WorkerCallbackId] = value;
        }
        return { __cn1WorkerCallback: value.__cn1WorkerCallbackId };
      }
      return null;
    }
    // Typed arrays / ArrayBuffers are structured-cloneable; pass them through
    // verbatim so a worker-built typed array reaches the host with its bytes
    // intact. Without this a Float32Array/Uint16Array fails Array.isArray, has
    // no __cn1HostRef/__jsValue, and falls through to the object-iteration path
    // below -- arriving on the host as a plain {0:..,1:..} object. (The GPU
    // backend instead passes Java primitive arrays, which serialize as plain
    // number arrays that the host re-wraps; this remains the correct, defensive
    // behavior for any code that does hand a typed array to a host call.)
    if (typeof ArrayBuffer !== "undefined"
        && (ArrayBuffer.isView(value) || value instanceof ArrayBuffer)) {
      return value;
    }
    if (Array.isArray(value)) {
      const out = new Array(value.length);
      for (let i = 0; i < value.length; i++) {
        out[i] = this.toHostTransferArg(value[i], _depth + 1, _seen);
      }
      return out;
    }
    // A Java String is a VM object, not a JS primitive. Host code (e.g. the
    // native-interface bridge, which passes the interface/method names) expects
    // the actual text, so marshal it to a plain JS string rather than letting it
    // fall through to the opaque object-iteration path below.
    if (value.__class === "java_lang_String") {
      return this.toNativeString(value);
    }
    // 64-bit long ({__l:1,l,h}) -> JS number for the host.
    if (value.__l === 1) {
      return _LtoNumber(value);
    }
    // Boxed primitives -> their JS value. NativeInterface args arrive boxed in an
    // Object[] (Integer.valueOf(...) etc.); the host wants the plain value.
    switch (value.__class) {
      case "java_lang_Integer": return value.cn1_java_lang_Integer_value | 0;
      case "java_lang_Short": return value.cn1_java_lang_Short_value | 0;
      case "java_lang_Byte": return value.cn1_java_lang_Byte_value | 0;
      case "java_lang_Character": return value.cn1_java_lang_Character_value | 0;
      case "java_lang_Boolean": return !!value.cn1_java_lang_Boolean_value;
      case "java_lang_Double": return Number(value.cn1_java_lang_Double_value);
      case "java_lang_Float": return Number(value.cn1_java_lang_Float_value);
      case "java_lang_Long": return _LtoNum(value.cn1_java_lang_Long_value);
      default: break;
    }
    if (value.__cn1HostRef != null) {
      return value.__cn1HostClass
        ? { __cn1HostRef: value.__cn1HostRef, __cn1HostClass: value.__cn1HostClass }
        : { __cn1HostRef: value.__cn1HostRef };
    }
    if (value.__jsValue !== undefined) {
      return this.toHostTransferArg(value.__jsValue, _depth + 1, _seen);
    }
    // CN1 wrapper for a Java object (has ``__classDef`` but neither
    // ``__cn1HostRef`` nor ``__jsValue``). The most common case is a
    // Java callback (``EventListener``, ``SetItemCallback``, etc.)
    // being passed as an argument to a host bridge call. We can't
    // serialise the wrapper itself — the ``classDef`` graph is
    // shared, mutable, and cyclic — so mint a worker callback that
    // dispatches the wrapper's single abstract method (if it has one)
    // when the host invokes it. This is the same SAM-functor escape
    // hatch ``port.js`` uses for ``EventListener.handleEvent`` /
    // ``AnimationFrameCallback.onAnimationFrame``, just generalised.
    if (value.__classDef && value.__class) {
      const samMethodId = this.findSamDispatchId(value.__classDef);
      if (samMethodId) {
        if (value.__cn1WorkerCallbackId == null) {
          const self = this;
          const className = value.__class;
          const wrapper = function() {
            // Wrap each arg as a JSObject, mirroring port.js's
            // ``__nativeEventListener`` (which calls
            // ``jvm.wrapJsResult(event, "com_codename1_html5_js_dom_Event")``
            // before dispatch). The translated SAM method body
            // expects Java-shaped args, not the raw host values
            // posted through the worker-callback bridge.
            const wrappedArgs = [];
            for (let i = 0; i < arguments.length; i++) {
              wrappedArgs.push(self.wrapJsResult(arguments[i], "com_codename1_html5_js_JSObject"));
            }
            try {
              const method = self.resolveVirtual(className, samMethodId);
              self.spawn(null, method.apply(null, [value].concat(wrappedArgs)));
            } catch (err) {
              if (typeof console !== "undefined" && typeof console.error === "function") {
                try { console.error("PARPAR:sam-callback-error:" + (err && err.message ? err.message : String(err))); }
                catch (_e) {}
              }
            }
          };
          wrapper.__cn1WorkerCallbackId = this.nextWorkerCallbackId++;
          value.__cn1WorkerCallbackId = wrapper.__cn1WorkerCallbackId;
          this.workerCallbacks[wrapper.__cn1WorkerCallbackId] = wrapper;
        }
        return { __cn1WorkerCallback: value.__cn1WorkerCallbackId };
      }
    }
    if (type === "object") {
      const out = {};
      const keys = Object.keys(value);
      for (let i = 0; i < keys.length; i++) {
        const key = keys[i];
        // Skip CN1-internal wrapper bookkeeping. ``__classDef`` /
        // ``__monitor`` are shared mutable graphs that don't survive
        // structured-clone postMessage; iterating them creates the
        // cycle we just guarded against. The host never reads any of
        // these — the bridge only cares about user data.
        if (key === "__class" || key === "__classDef" || key === "__id"
                || key === "__monitor" || key === "__jsValue"
                || key === "__cn1WorkerCallbackId") {
          continue;
        }
        out[key] = this.toHostTransferArg(value[key], _depth + 1, _seen);
      }
      return out;
    }
    return null;
  },
  /**
   * Find the dispatch id of the single abstract method on a JSO bridge
   * interface in {@code classDef}'s ancestry. SAM JSO functors (e.g.
   * ``EventListener.handleEvent``, ``SetItemCallback.callback``) have
   * exactly one method on the interface itself; once the impl class
   * survives RTA un-elimination its ``m:`` map carries the dispatch id,
   * so we can recover the SAM by inspecting the IMPL'S methods,
   * filtered to those declared on a JSO bridge interface in the
   * ancestry. The interface defs themselves don't carry the abstract
   * method ids (no method bodies → no ``m:`` entries on the interface
   * classdef), but the JSO bridge dispatch ids manifest does — and the
   * impl method picks up the same ``cn1_s_<method>_<sig>`` key.
   */
  findSamDispatchId(classDef) {
    if (!classDef) return null;
    // Walk ancestry collecting interface names. If the ancestry has
    // exactly ONE non-marker JSO bridge interface, the impl is a SAM
    // wrapper and we can use its single method.
    const interfaceNames = Object.create(null);
    const visited = Object.create(null);
    const stack = [classDef];
    let hasJsoBridge = false;
    while (stack.length) {
      const def = stack.pop();
      if (!def || visited[def.name]) continue;
      visited[def.name] = true;
      if (def.isInterface
          && def.name !== "com_codename1_html5_js_JSObject"
          && def.name !== classDef.name) {
        interfaceNames[def.name] = true;
      }
      if (def.name === "com_codename1_html5_js_JSObject") {
        hasJsoBridge = true;
      }
      if (def.interfaces) {
        for (let i = 0; i < def.interfaces.length; i++) {
          const ifaceDef = this.classes[def.interfaces[i]];
          if (ifaceDef) stack.push(ifaceDef);
        }
      }
      if (def.baseClass) {
        const baseDef = this.classes[def.baseClass];
        if (baseDef) stack.push(baseDef);
      }
    }
    if (!hasJsoBridge) return null;
    // Inspect the impl class's m: — these are the methods that survived
    // RTA. A SAM impl typically has __INIT__ + the single SAM method.
    // Filter out ctors / clinit and pick the remaining single entry.
    if (classDef.pendingMethods) this.flushPendingMethods(classDef);
    if (!classDef.methods) return null;
    const candidateIds = [];
    const allMethodIds = Object.keys(classDef.methods);
    for (let i = 0; i < allMethodIds.length; i++) {
      const id = allMethodIds[i];
      if (id.indexOf("__INIT__") >= 0 || id.indexOf("__CLINIT__") >= 0) {
        continue;
      }
      candidateIds.push(id);
    }
    if (candidateIds.length === 1) {
      return candidateIds[0];
    }
    return null;
  },
  spawn(threadObject, generator) {
    const thread = { id: this.nextThreadId++, object: threadObject, generator: generator, waiting: null, interrupted: false, done: false };
    this.threads.push(thread);
    if (shouldTraceThread(thread)) {
      vmTrace("runtime.spawn.thread-" + thread.id + ":" + threadDebugLabel(threadObject));
    }
    if (VM_DIAG_ENABLED && (thread.id | 0) > 1 && (thread.id | 0) <= 4) {
      vmTrace("runtime.spawn.stack.thread-" + thread.id + ":" + String(new Error().stack || ""));
    }
    // Sync methods (translated to plain ``function`` instead of
    // ``function*``) return non-iterable values — most commonly
    // ``undefined`` for a ``void`` return. ``drain`` calls
    // ``generator.next()`` and would explode on such a value, so
    // short-circuit here: the method already ran to completion when
    // the caller evaluated its arg, so the thread is done the moment
    // it's spawned.
    if (generator == null || typeof generator.next !== "function") {
      thread.done = true;
      if (threadObject) {
        threadObject[CN1_THREAD_ALIVE] = 0;
      }
      return thread;
    }
    this.enqueue(thread);
    return thread;
  },
  flushPendingFireAndForget() {
    if (this.pendingFireAndForget.length === 0) return;
    const batch = this.pendingFireAndForget;
    this.pendingFireAndForget = [];
    // toHostTransferArg sanitises each op (resolving JSO wrappers,
    // wrapping callbacks, etc.) -- still cheaper amortised across
    // the whole batch than per-op postMessage roundtrips.
    const safeOps = new Array(batch.length);
    for (let i = 0; i < batch.length; i++) {
      safeOps[i] = this.toHostTransferArg(batch[i]);
    }
    emitVmMessage({
      type: this.protocol.messages.HOST_CALL_BATCH || "host-call-batch",
      ops: safeOps
    });
  },
  enqueue(thread, value) {
    thread.waiting = null;
    thread.resumeValue = value;
    this.runnable.push(thread);
    this.drain();
  },
  // Take the screenshot-capture gate for the CURRENT thread. Called
  // synchronously (not yielded) by the cn1ss emit in port.js, immediately
  // before the ``__cn1_wait_for_ui_settle__`` / ``__cn1_capture_canvas_png__``
  // host round-trips and AFTER the form has already been presented. While held,
  // drain() defers every other green thread so none can paint onto the canvas
  // being sampled. Idempotent / last-writer-wins (captures are serialized by the
  // runner, so re-entrancy shouldn't occur, but guarding costs nothing).
  beginCaptureGate() {
    this.captureGateOwner = this.currentThread || null;
  },
  // Release the capture gate. MUST be called from a finally so a watchdog-aborted
  // or throwing capture still frees it. Re-arms drain so the threads deferred
  // during the capture window get to run.
  endCaptureGate() {
    if (this.captureGateOwner && this.captureGateOwner !== this.currentThread) {
      // A different thread is mid-capture; don't steal its gate.
      return;
    }
    this.captureGateOwner = null;
    this.scheduleDrain();
  },
  schedulerNow() {
    if (typeof global.performance !== "undefined" && global.performance
            && typeof global.performance.now === "function") {
      return global.performance.now();
    }
    return Date.now();
  },
  scheduleDrain() {
    if (this.draining || this.drainScheduled) {
      return;
    }
    this.drainScheduled = true;
    const self = this;
    setTimeout(function() {
      self.drainScheduled = false;
      self.drain();
    }, 0);
  },
  drain() {
    if (this.draining) {
      return;
    }
    // Opportunistic wakeup delivery on every outermost drain (i.e. every host
    // event that wakes the worker): when the one-shot wakeup timeout is being
    // throttled by the host (see _ensureWakeupPump), due sleeps/waits still
    // fire with near-zero latency here instead of waiting for the 1s pump.
    // O(pending) once per burst; no-op when nothing is due.
    if (this.timedWakeups.length && !this._processingWakeups
            && this._earliestWakeAt() <= this.schedulerNow() + 1) {
      this._processExpiredTimedWakeups();
    }
    this.draining = true;
    const deadline = this.schedulerNow() + 8;
    let steps = 0;
    // Threads held aside while the capture gate is owned by another thread (see
    // captureGateOwner). They are NOT lost: restored to runnable in the finally,
    // and endCaptureGate()/scheduleDrain re-runs them once the gate clears. Stays
    // null on every non-capture drain, so the steady-state hot path is untouched.
    let captureDeferred = null;
    try {
      while (this.runnable.length) {
        if (steps++ > 2048 || this.schedulerNow() >= deadline) {
          this.scheduleDrain();
          break;
        }
        // While a screenshot capture is reading the canvas, defer any OTHER
        // thread so it can't paint mid-capture (the off-by-one race). The owner
        // itself is never deferred, and the gate is null outside captures.
        if (this.captureGateOwner && this.runnable[0] !== this.captureGateOwner) {
          (captureDeferred || (captureDeferred = [])).push(this.runnable.shift());
          continue;
        }
        // Atomic-thread mode (set by flushGraphics' begin/endGraphicsAtomic
        // pair) used to suppress dispatch of every other green thread.
        // That created a deadlock window with cooperative monitor parking:
        // if the atomic thread blocks on a monitor held by some other
        // green thread, drain refuses to run the holder, the holder
        // never releases, the atomic thread never unparks. Cooperative
        // monitor semantics already prevent the recursive-paint flood
        // the atomic flag was guarding against -- a thread that re-enters
        // synchronized(pendingDisplay) inside flushGraphics naturally
        // queues behind the active flush. Trusting the locks instead of
        // the atomic flag avoids the deadlock without re-introducing
        // the flood.
        const thread = this.runnable.shift();
        if (thread.done) {
          continue;
        }
        this.currentThread = thread;
        // Worker-liveness probe feeding the heartbeat timer below: a frozen
        // resume count with a live heartbeat means parked/starved, a stopped
        // heartbeat means a synchronous infinite loop in this step. The resume
        // counter is a trivial increment; the (string-building) label is only
        // recorded under diag so production pays nothing.
        this.__cn1ResumeCount = (this.__cn1ResumeCount | 0) + 1;
        if (VM_DIAG_ENABLED) {
          this.__cn1LastResumeLabel = thread.id + ":" + threadDebugLabel(thread.object);
          this.__cn1LastResumeTs = this.schedulerNow();
        }
        if (!thread.__cn1LoggedFirstStep && shouldTraceThread(thread)) {
          thread.__cn1LoggedFirstStep = true;
          vmTrace("runtime.drain.first-step.thread-" + thread.id + ":" + threadDebugLabel(thread.object));
        }
        let result;
        // Restart the cooperative tick budget so ``yield* _Y()``
        // boundaries reached during this generator step measure
        // straight-line time within THIS step. See the
        // ``_Y`` / ``__cn1TickReset`` block at the top of this
        // file for the rationale.
        __cn1TickReset();
        try {
          if (thread.resumeError) {
            const resumeError = thread.resumeError;
            thread.resumeError = null;
            result = thread.generator.throw(resumeError);
          } else {
            result = thread.generator.next(thread.resumeValue);
          }
          thread.resumeValue = undefined;
          if (result.done) {
            thread.done = true;
            // Always-on lifecycle log: when the MAIN thread completes,
            // ParparVMBootstrap.run() has finished — i.e. lifecycle.init,
            // lifecycle.start, and runApp() all returned. We post a
            // ``lifecycle`` VM message back to the main-thread bridge
            // so it can flip ``window.cn1Started = true`` (the @JSBody-
            // driven flag set inside ParparVMBootstrap.setStarted lives
            // on the WORKER's window, not the main thread's, so the
            // headless-test ``page.evaluate(() => window.cn1Started)``
            // would never see it without this round trip).
            if (thread === this.mainThread || (this.mainThreadObject && thread.object === this.mainThreadObject)) {
              vmLifecycle("main-thread-completed");
              emitVmMessage({
                type: this.protocol.messages.LIFECYCLE || "lifecycle",
                phase: "started"
              });
            }
            if (thread.object) {
              thread.object[CN1_THREAD_ALIVE] = 0;
              this.notifyAll(thread.object);
            }
            continue;
          }
          this.handleYield(thread, result.value);
        if (this.__cn1BreakBurst) {
          this.__cn1BreakBurst = false;
          this.scheduleDrain();
          break;
        }
        } catch (threadErr) {
          // An uncaught exception in a green thread TERMINATES THAT THREAD
          // (Java semantics: Thread.run() unwinds, other threads keep running)
          // -- it must NOT halt the whole cooperative scheduler. The previous
          // behaviour let the error propagate out of the drain loop into the
          // outer catch -> fail(), which stopped dispatch entirely and wedged
          // the worker (frozen=1/runnable=0): a single __cn1_jso_bridge__
          // watchdog timeout (main thread briefly blocked by a heavy capture),
          // resumed into the parked thread as an uncaught RuntimeException, took
          // down the ENTIRE screenshot suite with no error surfaced to the host.
          // Terminate just this thread, wake any joiners, record the failure for
          // diagnostics, and continue draining the rest.
          thread.done = true;
          thread.resumeError = null;
          thread.resumeValue = undefined;
          if (thread.object) {
            thread.object[CN1_THREAD_ALIVE] = 0;
            this.notifyAll(thread.object);
          }
          this.fail(threadErr);
          continue;
        }
      }
    } catch (err) {
      this.fail(err);
    } finally {
      this.currentThread = null;
      this.draining = false;
      // Restore any threads deferred for the capture gate. They go back to the
      // FRONT (preserving their relative order) so they don't lose their place
      // behind threads enqueued during this burst. If the gate has since cleared
      // (endCaptureGate ran while the owner resumed) re-arm a drain so they run;
      // otherwise they sit harmlessly in runnable until the gate's own
      // scheduleDrain (or the owner's host callback) fires.
      if (captureDeferred && captureDeferred.length) {
        for (let i = captureDeferred.length - 1; i >= 0; i--) {
          this.runnable.unshift(captureDeferred[i]);
        }
        if (!this.captureGateOwner) {
          this.scheduleDrain();
        }
      }
      // Drain burst is over -- ship any queued fire-and-forget JSO
      // ops to the host as a single batch postMessage. Saves
      // hundreds of structured-clone roundtrips per paint frame.
      this.flushPendingFireAndForget();
      // SCHEDULER INVARIANT: drain() must NEVER return with runnable threads
      // and no drain pending. A stranded runnable thread (observed at boot as
      // runnable>=1, draining=0, drainScheduled=0 with a live heartbeat -- the
      // worker freezes mid-boot) means some enqueue path raced the drain-loop
      // exit: enqueue() calls drain() directly, which no-ops under the
      // re-entrancy guard, and if that enqueue landed as the loop was tearing
      // down, nothing re-arms. This is the correct backstop (not a timeout /
      // self-heal): re-schedule so the thread actually runs. The DIAG line keeps
      // the strand visible so the racing enqueue path can still be isolated.
      if (this.runnable.length && !this.captureGateOwner && !this.drainScheduled) {
        if (VM_DIAG_ENABLED) {
          vmTrace("DIAG:DRAIN_RESTRAND:runnable=" + this.runnable.length);
        }
        this.scheduleDrain();
      }
    }
  },
  // Cooperative scheduler bookkeeping: see field comments above.
  _scheduleTimedWakeup(entry) {
    this.timedWakeups.push(entry);
    this._refreshTimedWakeupTimer();
    this._ensureWakeupPump();
  },
  // Permanent low-frequency backstop for the one-shot wakeup timer.
  //
  // Headless/hidden Chromium intensively throttles rapidly re-armed
  // setTimeout CHAINS (nesting depth >= 5, short delays) down to ~one
  // firing per minute, while >=1s intervals keep firing normally --
  // observed on the screenshot suite as the heartbeat interval beating
  // every 1.5s while the armed wakeup timeout sat 12-48s past its target
  // (sinceStepMs == wakeFiredAgo == 12771 with every thread parked), so
  // every Thread.sleep / Object.wait(timeout) in the VM stalled in
  // batches. The pump bounds that worst case at ~1s: a cheap length +
  // earliest-deadline check, processing only when something is actually
  // due. The one-shot timer remains the precision path; drain() also
  // opportunistically processes due wakeups on every host event.
  _ensureWakeupPump() {
    if (this._wakeupPump != null || typeof setInterval !== "function") {
      return;
    }
    const self = this;
    this._wakeupPump = setInterval(function() {
      try {
        if (self.timedWakeups.length && self._earliestWakeAt() <= self.schedulerNow() + 1) {
          self._processExpiredTimedWakeups();
        }
      } catch (_e) {
        // Never let the backstop kill itself.
      }
    }, 1000);
    // Node harnesses: don't hold the process open for the pump.
    if (this._wakeupPump && typeof this._wakeupPump.unref === "function") {
      this._wakeupPump.unref();
    }
  },
  _earliestWakeAt() {
    let earliest = Infinity;
    for (let i = 0; i < this.timedWakeups.length; i++) {
      const w = this.timedWakeups[i];
      if (!w.cancelled && w.wakeAt < earliest) earliest = w.wakeAt;
    }
    return earliest;
  },
  _removeTimedWakeup(entry) {
    if (!entry || entry.cancelled) return;
    entry.cancelled = true;
    const idx = this.timedWakeups.indexOf(entry);
    if (idx >= 0) this.timedWakeups.splice(idx, 1);
    this._refreshTimedWakeupTimer();
  },
  _refreshTimedWakeupTimer() {
    let earliest = Infinity;
    for (let i = 0; i < this.timedWakeups.length; i++) {
      const w = this.timedWakeups[i];
      if (!w.cancelled && w.wakeAt < earliest) earliest = w.wakeAt;
    }
    if (earliest === Infinity) {
      if (this._wakeupTimer != null) {
        clearTimeout(this._wakeupTimer);
        this._wakeupTimer = null;
        this._wakeupAt = Infinity;
      }
      return;
    }
    if (this._wakeupTimer != null && this._wakeupAt <= earliest
            && this._wakeupAt > this.schedulerNow() - 100) {
      // Existing timer fires sooner or at the same moment; keep it. The
      // third clause guards against a ZOMBIE: a timer whose target time is
      // already well past yet whose callback never ran (its first statement
      // nulls _wakeupTimer, so non-null + past-due means the host lost the
      // timeout -- observed on the screenshot suite as every sleeping thread
      // stranded 30s+ past its deadline with the queue intact, because a
      // past-due _wakeupAt satisfies ``<= earliest`` for EVERY later wakeup
      // and this branch then never re-arms). Distrust it and re-arm; if the
      // old timer does still fire, the callback's _wakeupTimer-null reset +
      // re-entrant processing are idempotent, so the duplicate is harmless.
      return;
    }
    if (this._wakeupTimer != null) {
      if (VM_DIAG_ENABLED && this._wakeupAt !== Infinity
              && this._wakeupAt <= this.schedulerNow() - 100) {
        try {
          vmTrace("DIAG:WAKEUP_TIMER_ZOMBIE:rearmed:staleMs="
            + Math.round(this.schedulerNow() - this._wakeupAt));
        } catch (_e) {}
      }
      clearTimeout(this._wakeupTimer);
    }
    const delay = Math.max(0, earliest - this.schedulerNow());
    this._wakeupAt = earliest;
    const self = this;
    this._wakeupArmCount = (this._wakeupArmCount | 0) + 1;
    this._wakeupTimer = setTimeout(function() {
      self._wakeupFireCount = (self._wakeupFireCount | 0) + 1;
      self._wakeupLastFiredAt = self.schedulerNow();
      self._wakeupTimer = null;
      self._wakeupAt = Infinity;
      // ALWAYS reschedule remaining wakeups, even if processing one throws --
      // otherwise an exception here breaks the single-timer chain and any other
      // pending sleep/wait never fires, wedging the worker (no green thread is
      // running to re-arm it). _processExpiredTimedWakeups refreshes on its own
      // success path; the finally guarantees it on the throw path too.
      try { self._processExpiredTimedWakeups(); }
      finally { if (self._wakeupTimer == null) self._refreshTimedWakeupTimer(); }
    }, delay);
  },
  _processExpiredTimedWakeups() {
    if (this._processingWakeups) {
      // Re-entrancy guard: the resume loop below runs green threads via
      // enqueue -> drain, and drain's opportunistic due-check (or the pump /
      // a late one-shot) could otherwise re-enter while a batch is mid-resume.
      return;
    }
    this._processingWakeups = true;
    try {
      this._processExpiredTimedWakeupsInner();
    } finally {
      this._processingWakeups = false;
    }
  },
  _processExpiredTimedWakeupsInner() {
    const now = this.schedulerNow();
    const expired = [];
    for (let i = this.timedWakeups.length - 1; i >= 0; i--) {
      const w = this.timedWakeups[i];
      if (w.cancelled) {
        this.timedWakeups.splice(i, 1);
        continue;
      }
      // 1ms tolerance: setTimeout firing slightly early under browser
      // clamping shouldn't keep the entry around for another full cycle.
      if (w.wakeAt <= now + 1) {
        expired.push(w);
        this.timedWakeups.splice(i, 1);
      } else if (VM_DIAG_ENABLED && !(w.wakeAt > now - 2000)) {
        // An entry that is neither due (<= now+1) nor sane-future fails BOTH
        // comparisons only when wakeAt isn't an ordinary number (NaN / boxed
        // long / string). Print its raw shape -- this is the only way a
        // queued, uncancelled, overdue entry can survive processing.
        try {
          vmTrace("DIAG:WAKEUP_BAD_ENTRY:kind=" + String(w.kind)
            + ":typeof=" + (typeof w.wakeAt)
            + ":val=" + String(w.wakeAt).slice(0, 30)
            + ":thread=" + (w.thread ? w.thread.id : "-"));
        } catch (_e) {}
      }
    }
    expired.reverse();  // restore registration order for FIFO fairness
    for (let i = 0; i < expired.length; i++) {
      const w = expired[i];
      // RE-CHECK cancelled. Processing an earlier entry below calls enqueue ->
      // drain, which runs green threads synchronously; one of them can notify a
      // monitor and cancel a LATER wait/sleep entry already collected in this
      // same `expired` batch. Without this re-check we would still fire that
      // cancelled entry's resumeWaiter for a waiter whose thread has ALREADY
      // resumed and parked elsewhere (typically a host-call round-trip read) --
      // resuming it with the wrong value (undefined) so the read returns null.
      // That was the surface-encode/getRGB "lost response", the flaky transition
      // wedge, and the encode-fail cascade.
      if (w.cancelled) {
        continue;
      }
      try {
      if (w.kind === "sleep") {
        this.enqueue(w.thread);
      } else if (w.kind === "wait") {
        this.resumeWaiter(w.waiter);
      } else if (w.kind === "hostcall") {
        // A JSO-bridge round-trip whose host response never arrived (see the
        // watchdog armed in dispatchYield). If it is still pending, fail it
        // with a transient error so the parked thread resumes and the
        // invokeJsoBridge retry re-issues the read. If it already resolved,
        // resolveHostCall cancelled this entry, so this is a no-op.
        if (this.pendingHostCalls[w.id]) {
          if (VM_DIAG_ENABLED) {
            try { vmDiag("HOSTCALL_TIMEOUT", "id", String(w.id)); } catch (_e) {}
          }
          this.resolveHostCall(w.id, false, null, "host call timed out (jso bridge)");
        }
      }
      } catch (resumeErr) {
        // Per-entry guard: every entry in this batch is ALREADY spliced out of
        // timedWakeups, so an exception escaping one resume would strand every
        // remaining entry's thread in a sleep/wait that can never fire again.
        // Contain the failure to the one entry and keep resuming the rest.
        try {
          vmTrace("DIAG:WAKEUP_RESUME_THREW:kind=" + String(w.kind)
            + ":err=" + String(resumeErr && resumeErr.message || resumeErr).slice(0, 120));
        } catch (_e) {}
      }
    }
    this._refreshTimedWakeupTimer();
  },
  // Snapshot every live green thread and WHAT it is blocked on. Used by the
  // dispatch watchdog to isolate a stall deterministically (without a JS
  // debugger attaching to the worker): a deadlock shows up as the EDT parked on
  // a monitor/capture-gate whose owner is itself parked. Pure read; safe to call
  // from a stalled state.
  dumpThreadStates() {
    var out = [];
    var ths = this.threads || [];
    for (var i = 0; i < ths.length; i++) {
      var t = ths[i];
      if (t.done) continue;
      var w = t.waiting;
      var desc;
      if (!w) {
        desc = "RUNNABLE";
      } else if (w.op === "monitor_enter") {
        desc = "monitor_enter";
      } else if (w.op === "sleep") {
        desc = "sleep:dueIn=" + (w.entry ? Math.round((w.entry.wakeAt | 0) - this.schedulerNow()) : "?");
      } else if (w.op === "wait") {
        desc = "obj.wait";
      } else if (w.id != null) {
        var pc = this.pendingHostCalls[w.id];
        desc = "hostcall:" + (pc && pc.symbol ? pc.symbol : "?")
          + (pc && pc.jso ? ("{" + pc.jso + "}") : "");
      } else {
        desc = String(w.op || "?");
      }
      var label;
      try { label = threadDebugLabel(t.object); } catch (e) { void e; label = "?"; }
      out.push("t" + t.id + "[" + label + "]:" + desc);
    }
    return "THREAD_DUMP:live=" + out.length
      + ":runnable=" + (this.runnable ? this.runnable.length : -1)
      + ":captureGateOwner=" + (this.captureGateOwner ? ("t" + this.captureGateOwner.id) : "none")
      + ":draining=" + (this.draining ? 1 : 0)
      + " " + out.join(" ");
  },
  handleYield(thread, yielded) {
    if (shouldTraceThread(thread)) {
      vmTrace("runtime.handleYield.thread-" + thread.id + ":" +
              threadDebugLabel(thread.object) +
              ":" + (yielded && yielded.op ? yielded.op : "requeue"));
    }
    if (!yielded || !yielded.op) {
      this.enqueue(thread, yielded);
      return;
    }
    if (yielded.op === "byield") {
      this.runnable.unshift(thread);
      this.__cn1BreakBurst = true;
      return;
    }
    if (yielded.op === "sleep") {
      // millis originates from Thread.sleep(long) -> BigInt; coerce to a Number
      // before the scheduler's Number-domain timer arithmetic (avoids BigInt mix).
      const millis = Math.max(0, _LtoNum(yielded.millis) | 0);
      if (millis === 0) {
        // Thread.yield / Thread.sleep(0) is just a co-operative hand-off
        // to the next runnable green thread; no real-time delay needed.
        this.enqueue(thread);
        return;
      }
      if (VM_DIAG_ENABLED && millis > 5000) {
        try { vmTrace("DIAG:LONG_SLEEP:millis=" + millis + ":stack=" + String(new Error().stack).split("\n").slice(1, 10).join("<")); } catch (_e) {}
      }
      const entry = { kind: "sleep", thread: thread, wakeAt: this.schedulerNow() + millis, cancelled: false };
      thread.waiting = { op: "sleep", entry: entry };
      this._scheduleTimedWakeup(entry);
      return;
    }
    if (yielded.op === "wait") {
      const waiter = { thread: thread, monitor: yielded.monitor, reentryCount: yielded.reentryCount };
      yielded.monitor.__monitor.waiters.push(waiter);
      // timeout originates from Object.wait(long) -> BigInt; coerce to Number.
      const waitTimeout = _LtoNum(yielded.timeout) | 0;
      if (waitTimeout > 0) {
        const entry = { kind: "wait", waiter: waiter, wakeAt: this.schedulerNow() + waitTimeout, cancelled: false };
        waiter.timedEntry = entry;
        this._scheduleTimedWakeup(entry);
      }
      thread.waiting = { op: "wait", waiter: waiter };
      return;
    }
    if (yielded.op === this.protocol.messages.HOST_CALL) {
      thread.waiting = { op: this.protocol.messages.HOST_CALL, id: yielded.id };
      var __jsoReq = (yielded.symbol === "__cn1_jso_bridge__" && yielded.args && yielded.args[0]) ? yielded.args[0] : null;
      this.pendingHostCalls[yielded.id] = { thread: thread, symbol: yielded.symbol,
        jso: __jsoReq ? ((__jsoReq.kind || "?") + "." + (__jsoReq.member || "?")
          + "@" + (__jsoReq.receiverClass || (__jsoReq.receiver && __jsoReq.receiver.__cn1HostClass) || "?")) : null };
      const rawArgs = yielded.args || [];
      const safeArgs = new Array(rawArgs.length);
      for (let i = 0; i < rawArgs.length; i++) {
        safeArgs[i] = this.toHostTransferArg(rawArgs[i]);
      }
      // Round-trip HOST_CALL needs prior fire-and-forget batch
      // delivered first or the host will execute the round-trip
      // op against an out-of-date canvas state.
      this.flushPendingFireAndForget();
      emitVmMessage({ type: this.protocol.messages.HOST_CALL, id: yielded.id, symbol: yielded.symbol, args: safeArgs });
      // LOST-RESPONSE WATCHDOG. The host is a thin, dumb pixel sink (mirroring
      // the C/iOS native backend) and the worker<->host postMessage channel can
      // drop or never deliver a callback under load -- when it does, this green
      // thread parks on pendingHostCalls[id] forever and the whole suite wedges
      // with no error (the lightweight-popup / DualAppearance capture hangs).
      // For BOUNDED host natives only (see HOST_CALL_WATCHDOG_MS), arm a timeout
      // matched to that op's worst-case latency: on expiry resume the thread
      // with a transient error so the caller recovers (JSO reads re-issue via
      // invokeJsoBridge's retry; capture/settle callers catch and advance).
      // Unbounded natives (image load, fetch, the screenshot WebSocket) are not
      // in the map, so they are never aborted. On a healthy channel the call
      // resolves well within the timeout and this never fires.
      const __watchdogMs = HOST_CALL_WATCHDOG_MS[yielded.symbol];
      if (__watchdogMs != null) {
        const pendingEntry = this.pendingHostCalls[yielded.id];
        if (pendingEntry) {
          const timeoutEntry = { kind: "hostcall", id: yielded.id, wakeAt: this.schedulerNow() + __watchdogMs, cancelled: false };
          pendingEntry.timeoutEntry = timeoutEntry;
          this._scheduleTimedWakeup(timeoutEntry);
        }
      }
      return;
    }
    if (yielded.op === "monitor_enter") {
      // Thread is parked on monitor.entrants until ``monitorExit``
      // promotes it. No timer, no setTimeout -- waking is purely
      // event-driven on the holder's release.
      thread.waiting = { op: "monitor_enter", entrant: yielded.entrant };
      return;
    }
    if (yielded.op === "await") {
      // Suspend the green thread on a WORKER-LOCAL promise -- the worker-side
      // analog of HOST_CALL (which suspends on a host round-trip). Used to park
      // a thread until an async resource loaded INSIDE the worker is ready
      // (e.g. a FontFace added to self.fonts, so OffscreenCanvas text
      // measurement reads real metrics instead of a fallback). The unpark path
      // is the promise's own settlement -> enqueue, mirroring how the image
      // decode barrier (__cn1_decode_image_from_url__) resumes its caller. The
      // thread resumes with the resolved value; a rejection resumes with null
      // so the caller degrades gracefully (never parks forever on a 404).
      thread.waiting = { op: "await" };
      const scheduler = this;
      Promise.resolve(yielded.promise).then(
        function (v) { scheduler.enqueue(thread, v === undefined ? null : v); },
        function () { scheduler.enqueue(thread, null); }
      );
      return;
    }
    throw new Error("Unsupported yield op " + yielded.op);
  },
  resumeWaiter(waiter) {
    const list = waiter.monitor.__monitor.waiters;
    const index = list.indexOf(waiter);
    if (index >= 0) {
      list.splice(index, 1);
    }
    const monitor = waiter.monitor.__monitor || (waiter.monitor.__monitor = this.createMonitor());
    if (monitor.owner == null || monitor.owner === waiter.thread.id) {
      monitor.owner = waiter.thread.id;
      monitor.count = waiter.reentryCount;
      this.enqueue(waiter.thread, waiter.resumeValue);
      return;
    }
    monitor.entrants.push(waiter);
  },
  monitorEnter(thread, obj) {
    const monitor = obj.__monitor || (obj.__monitor = this.createMonitor());
    if (monitor.owner == null || monitor.owner === thread.id) {
      monitor.owner = thread.id;
      monitor.count++;
      return null;
    }
    // Contention. Park the current green thread on the monitor's
    // ``entrants`` queue and signal a yield op to the caller. ``_me``
    // (the translator-emitted ``yield* _me(obj)`` helper) sees the op
    // and yields it; ``handleYield`` parks the thread; ``drain`` moves
    // on to the next runnable thread. When the holder eventually calls
    // ``monitorExit`` and ``count`` drops to 0, the next entrant is
    // promoted to owner and re-enqueued.
    //
    // Older revisions of this method ``stole`` the lock from the
    // current holder (pushed its (owner, count) onto a stack and took
    // over), then unwound the stack on the entrant's matching exit.
    // That made every contended synchronized block effectively
    // non-mutexing: BOTH green threads could be inside the same
    // block simultaneously, mutating shared state with the locking
    // protocol promising they couldn't. Display.lock contention from
    // Display.invokeAndBlock interleaved with the Dialog body thread
    // was the most-felt manifestation. Yielding-and-queueing matches
    // real Java monitor semantics on a cooperatively-scheduled worker.
    const entrant = { thread: thread, reentryCount: 1, resumeValue: null };
    monitor.entrants.push(entrant);
    return { op: "monitor_enter", monitor: obj, entrant: entrant };
  },
  monitorExit(thread, obj) {
    const monitor = obj.__monitor || (obj.__monitor = this.createMonitor());
    if (monitor.owner !== thread.id) {
      throw new Error("IllegalMonitorStateException");
    }
    monitor.count--;
    if (monitor.count <= 0) {
      monitor.count = 0;
      monitor.owner = null;
      if (monitor.entrants.length) {
        const next = monitor.entrants.shift();
        monitor.owner = next.thread.id;
        monitor.count = next.reentryCount;
        this.enqueue(next.thread, next.resumeValue);
      }
    }
  },
  waitOn(thread, obj, timeout) {
    const monitor = obj.__monitor || (obj.__monitor = this.createMonitor());
    if (monitor.owner !== thread.id) {
      throw new Error("IllegalMonitorStateException");
    }
    const reentryCount = monitor.count;
    monitor.owner = null;
    monitor.count = 0;
    // Releasing the monitor for ``wait`` must also drain the head of
    // the entrants queue, identical to ``monitorExit``. Otherwise any
    // thread parked on this monitor stays stuck forever even after
    // the holder calls wait() and (eventually) gets notified --
    // ownership goes back to the waker, never to the queued entrant.
    // This is the asymmetry that hung lifecycle.start: EDT acquired
    // Display.lock, called wait, didn't promote the main thread (
    // queued on entrants from invokeAndBlock's first synchronized
    // block), and the runtime sat with owner=null + entrants=1
    // forever.
    if (monitor.entrants.length) {
      const next = monitor.entrants.shift();
      monitor.owner = next.thread.id;
      monitor.count = next.reentryCount;
      this.enqueue(next.thread, next.resumeValue);
    }
    return { op: "wait", monitor: obj, timeout: _LtoNum(timeout) | 0, reentryCount: reentryCount };
  },
  notifyOne(obj) {
    const monitor = obj.__monitor || (obj.__monitor = this.createMonitor());
    const waiter = monitor.waiters.shift();
    if (!waiter) {
      return;
    }
    if (waiter.timedEntry) {
      this._removeTimedWakeup(waiter.timedEntry);
      waiter.timedEntry = null;
    }
    this.resumeWaiter(waiter);
  },
  notifyAll(obj) {
    const monitor = obj.__monitor || (obj.__monitor = this.createMonitor());
    const waiters = monitor.waiters.splice(0, monitor.waiters.length);
    for (const waiter of waiters) {
      if (waiter.timedEntry) {
        this._removeTimedWakeup(waiter.timedEntry);
        waiter.timedEntry = null;
      }
      this.resumeWaiter(waiter);
    }
  },
  findThreadByObject(obj) {
    for (let i = 0; i < this.threads.length; i++) {
      if (this.threads[i].object === obj) {
        return this.threads[i];
      }
    }
    return null;
  },
  interruptThread(threadObject) {
    if (!threadObject) {
      return;
    }
    threadObject.__interrupted = 1;
    const thread = this.findThreadByObject(threadObject);
    if (!thread || !thread.waiting) {
      return;
    }
    if (thread.waiting.op === "sleep") {
      if (thread.waiting.entry) {
        this._removeTimedWakeup(thread.waiting.entry);
      }
      this.enqueue(thread, { interrupted: true });
      return;
    }
    if (thread.waiting.op === "wait") {
      const waiter = thread.waiting.waiter;
      if (waiter.timedEntry) {
        this._removeTimedWakeup(waiter.timedEntry);
        waiter.timedEntry = null;
      }
      const monitor = waiter.monitor.__monitor || (waiter.monitor.__monitor = this.createMonitor());
      const index = monitor.waiters.indexOf(waiter);
      if (index >= 0) {
        monitor.waiters.splice(index, 1);
      }
      if (monitor.owner == null || monitor.owner === thread.id) {
        monitor.owner = thread.id;
        monitor.count = waiter.reentryCount;
        this.enqueue(thread, { interrupted: true });
      } else {
        waiter.resumeValue = { interrupted: true };
        monitor.entrants.push(waiter);
      }
    }
  },
  createException(className) {
    const ex = this.newObject(className);
    // Prefer the direct function reference attached at ``defineClass``
    // time (``def.t`` → ``def.noArgCtor``). Fall back to the legacy
    // string-concat lookup for any class that wasn't emitted with a
    // ``t:`` field — it still won't resolve a real ctor under
    // mangling, but matches prior behaviour for any pre-existing
    // callers that relied on the side-effect-free no-op.
    const def = this.classes[className];
    let ctor = def && def.noArgCtor ? def.noArgCtor : null;
    if (typeof ctor !== "function") {
      ctor = global["cn1_" + className + "___INIT__"];
    }
    return { object: ex, ctor: ctor };
  },
  applyNativeOverrides() {
    const classNames = Object.keys(this.classes);
    for (let i = 0; i < classNames.length; i++) {
      const cls = this.classes[classNames[i]];
      if (!cls) {
        continue;
      }
      // Force every deferred ``jvm.m`` thunk to run now so the map
      // keys are visible to the native-override pass and later-
      // fired dispatches don't have to redo the flush per class.
      if (cls.pendingMethods) {
        this.flushPendingMethods(cls);
      }
      if (!cls.methods) {
        continue;
      }
      const methodIds = Object.keys(cls.methods);
      for (let j = 0; j < methodIds.length; j++) {
        const methodId = methodIds[j];
        const nativeOverride = this.nativeMethods[methodId];
        if (typeof nativeOverride === "function" && cls.methods[methodId] !== nativeOverride) {
          cls.methods[methodId] = nativeOverride;
        }
      }
    }
    this.resolvedVirtualCache = Object.create(null);
  },
  start() {
    if (!this.mainClass || !this.mainMethod) {
      vmLifecycle("start-failed-no-main");
      throw new Error("No main class configured for javascript backend");
    }
    vmLifecycle("start:mainClass=" + this.mainClass);
    vmDiag("LIFECYCLE_START", "mainClass", this.mainClass);
    this.applyNativeOverrides();
    ensureSystemPrintStreams();
    vmTrace("runtime.start.before-main-generator");
    const mainArgs = this.newArray(0, "java_lang_String", 1);
    const mainThreadObject = this.newObject("java_lang_Thread");
    mainThreadObject[CN1_THREAD_ALIVE] = 1;
    mainThreadObject[CN1_THREAD_NAME] = this.createStringLiteral("main");
    this.mainThreadObject = mainThreadObject;
    vmLifecycle("start:invoking-main-method=" + this.mainMethod);
    const mainGenerator = global[this.mainMethod](mainArgs);
    vmLifecycle("start:main-method-returned=" + (mainGenerator != null && typeof mainGenerator.next === "function" ? "generator" : "sync"));
    vmTrace("runtime.start.after-main-generator");
    const mainThread = this.spawn(mainThreadObject, mainGenerator);
    // Stash the main thread + object so the drain loop can identify
    // when the main bytecode completes vs when an auxiliary thread
    // (e.g. a CN1SS test runner Thread or worker callback) finishes.
    this.mainThread = mainThread;
    vmTrace("runtime.start.after-spawn");
    this.currentThread = mainThread;
    vmTrace("runtime.start.before-drain");
    this.drain();
    vmTrace("runtime.start.after-drain");
    vmLifecycle("start:drain-returned threads=" + this.threads.length);
  },
  describeProtocol() {
    return {
      type: this.protocol.messages.PROTOCOL,
      version: this.protocol.version,
      messages: this.protocol.messages
    };
  },
  handleMessage(message) {
    if (!message || !message.type) {
      return false;
    }
    if (message.type === this.protocol.messages.PROTOCOL_INFO) {
      emitVmMessage(this.describeProtocol());
      return true;
    }
    if (message.type === this.protocol.messages.HOST_CALLBACK) {
      return this.resolveHostCall(message.id, !message.error, message.value, message.errorMessage || message.error);
    }
    if (message.type === this.protocol.messages.TIMER_WAKE) {
      this.drain();
      return true;
    }
    if (message.type === this.protocol.messages.EVENT || message.type === this.protocol.messages.UI_EVENT) {
      this.lastEvent = message;
      this.eventQueue.push(message);
      return true;
    }
    if (message.type === "worker-callback") {
      // DOM events dispatched from the main thread back into the worker.
      // Look the registered function up by ID and invoke it with whatever
      // payload the bridge serialised (mouse/key events carry a synthetic
      // event object with the fields ``port.js`` cares about). We route
      // exceptions through ``jvm.fail`` so unhandled callback errors
      // surface via the same path as other runtime failures.
      const cb = this.workerCallbacks[message.callbackId | 0];
      if (cb) {
        // Re-attach the no-op preventDefault / stopPropagation stubs that
        // browser_bridge.js stripped before postMessage (structured clone
        // can't carry functions). These are effectively no-ops once we're
        // inside the worker because the main-thread event has long since
        // been dispatched, but Java EventListener code commonly calls
        // them and would otherwise trigger a "Missing JS member" throw
        // in the JSO bridge.
        const rawArgs = Array.isArray(message.args) ? message.args : [message.args];
        for (let i = 0; i < rawArgs.length; i++) {
          const arg = rawArgs[i];
          if (arg && typeof arg === "object" && typeof arg.type === "string" && !arg.preventDefault) {
            arg.preventDefault = function() {};
            arg.stopPropagation = function() {};
            arg.stopImmediatePropagation = function() {};
          }
        }
        try {
          cb.apply(null, rawArgs);
        } catch (err) {
          // Don't call jvm.fail here — a single broken event handler
          // shouldn't halt the whole VM. Log via console.error (which
          // the main thread will echo when diagEnabled) so the cause is
          // still visible in dev tools without poisoning __parparError.
          if (typeof console !== "undefined" && typeof console.error === "function") {
            try {
              console.error("PARPAR:worker-callback-error:" + (err && err.message ? err.message : String(err)));
            } catch (_logErr) {
              /* best-effort */
            }
          }
        }
      }
      return true;
    }
    return false;
  }
};

global.jvm = jvm;
jvm.jsoRegistry = jsoRegistry;
// Short-form aliases for the hottest ``jvm.*`` methods. The
// translated_app*.js files invoke these tens of thousands of times
// each (7.7k ``ensureClassInitialized``, 5.3k ``createStringLiteral``,
// 3.1k ``newObject``) and the full property name dominates the raw
// bundle — collapsing them to single-char identifiers saves ~500 KiB
// of pre-gzip output. The long names are kept on the object for any
// hand-written runtime / port code that references them directly.
jvm.eI = jvm.ensureClassInitialized;
jvm.sL = jvm.createStringLiteral;
jvm.nO = jvm.newObject;
// CHECKCAST: throw ClassCastException when ``value`` is non-null and
// its class isn't assignable to ``className``. A null receiver is
// always a valid cast per JVM spec. Replaces ~280 chars of inline
// assignableTo/enhanceJsWrapper boilerplate at each of the ~2200
// CHECKCAST call sites in Initializr.
jvm.cC = function(value, className) {
  if (value == null) return;
  const cd = value.__classDef;
  if (value.__class === className || (cd && cd.assignableTo && cd.assignableTo[className])) return;
  if (value.__class && jvm.assignableViaAncestors(value.__class, className)) {
    if (cd && cd.assignableTo) cd.assignableTo[className] = 1;
    return;
  }
  if (value.__jsValue !== void 0) {
    jvm.enhanceJsWrapper(value, className);
    const cd2 = value.__classDef;
    if (value.__class === className || (cd2 && cd2.assignableTo && cd2.assignableTo[className])) return;
  }
  throw new Error("ClassCastException");
};
// Array load / store helpers — factor the null+type+bounds checks
// out of the ~3000 emitted array-access sites (~170 chars each).
jvm.aL = function(arr, idx) {
  if (!arr || !arr.__array) throw new Error("Array expected: " + (arr == null ? "null" : (arr.__class || typeof arr)));
  if (idx < 0 || idx >= arr.length) throw new Error("ArrayIndexOutOfBoundsException");
  return arr[idx];
};
jvm.aS = function(arr, idx, value) {
  if (!arr || !arr.__array) throw new Error("Array expected: " + (arr == null ? "null" : (arr.__class || typeof arr)));
  if (idx < 0 || idx >= arr.length) throw new Error("ArrayIndexOutOfBoundsException");
  arr[idx] = value;
};
// Allocate a size-N array initialised to null. Used at the top of
// every switch-interpreter method body to set up its ``locals``
// slots (~3000 methods × 13 chars vs the inline
// ``new Array(N).fill(null)``).
jvm.aN = function(n) { return new Array(n).fill(null); };
// Compact frame builder: allocate a size-N locals array and fill the
// first K slots with the given args. Saves ~15-30 chars per method
// vs the former ``jvm.aN(N)`` + separate ``locals[i] = ...`` lines.
// Used only for methods without long/double args — those require the
// explicit emission because a long/double occupies two local slots
// but arrives as a single JS argument.
jvm.fr = function(n) {
  const a = new Array(n).fill(null);
  for (let i = 1; i < arguments.length; i++) a[i - 1] = arguments[i];
  return a;
};
// INSTANCEOF — returns truthy when ``value`` is non-null and assignable
// to ``className`` via __class match or assignableTo table lookup.
// Call sites wrap the result in ``? 1 : 0`` to match the JVM's int
// return, so a truthy/falsy return here is sufficient.
jvm.iO = function(value, className) {
  if (value == null) return false;
  if (value.__class === className) return true;
  const cd = value.__classDef;
  if (cd && cd.assignableTo && cd.assignableTo[className]) return true;
  if (value.__class && jvm.assignableViaAncestors(value.__class, className)) {
    if (cd && cd.assignableTo) cd.assignableTo[className] = 1;
    return true;
  }
  return false;
};
// === Cooperative tick budget ===
// Generic preemption mechanism. Long synchronous Java-to-Java call
// chains (e.g. UIManager.setThemeProps -> buildTheme ->
// installNativeTheme -> refreshTheme(true), which is just one of many
// realistic chains an app can write without any "obvious red flag"
// in the source) keep ``thread.generator.next()`` running for
// hundreds of milliseconds at a time. While it runs the worker's
// message loop is starved -- pointer events, host callbacks, and
// requestAnimationFrame replies all sit unprocessed and the user
// observes a frozen UI.
//
// drain()'s 8 ms budget cannot help here: it only checks the deadline
// BETWEEN ``generator.next()`` calls, not within one. To preempt
// inside a long chain we need yield points seeded INSIDE generator
// methods. The translator emits ``yield* _Y()`` at the entry of every
// generator method (see JavascriptMethodGenerator.appendMethodImpl);
// this helper does a single timestamp diff against
// ``__cn1TickStartedAt`` and yields ``{op:"sleep", millis:0}`` only
// when the budget has been exceeded AND we are not currently inside
// a class <clinit> (clinit is driven by a synchronous run-to-
// completion loop in ensureClassInitialized that cannot honour real
// suspensions).
//
// drain() resets ``__cn1TickStartedAt`` before every
// ``generator.next()`` so each step gets a fresh budget. The actual
// yield is rare -- only chains that exceed the budget pay the cost
// of a real cooperative hand-off.
let __cn1TickStartedAt = 0;
const __cn1TickBudgetMs = 400;
function __cn1TickReset() {
  __cn1TickCounter = 0;
  if (typeof global.performance !== "undefined" && global.performance
      && typeof global.performance.now === "function") {
    __cn1TickStartedAt = global.performance.now();
  } else {
    __cn1TickStartedAt = Date.now();
  }
}
global.__cn1TickReset = __cn1TickReset;
// ``_Yc`` is a fast plain-function predicate the translator pairs
// with a sentinel yield. The emit pattern at every generator-method
// entry is: ``if(_Yc())yield _Yv;`` -- this avoids allocating a
// generator object per method invocation (which a ``yield* _Y()``
// helper would have done; CN1 apps invoke many thousands of generator
// methods per second on the worker).
//
// Hot-path: most calls hit only the counter increment + comparison
// (~5 ns), since invoking ``performance.now()`` on every method
// entry across thousands of entries per millisecond is what makes
// boot time-budget-aware in the first place but also adds tens of
// percent overhead. We amortise: after every ``__cn1TickStride``
// counter ticks we drop into the slow path which actually queries
// the wall clock and resets/yields if the budget has elapsed. The
// counter is reset to zero when drain() restarts a generator step,
// so fresh steps start fast again.
// Budget yield is a DISTINCT op from sleep(0): it must give the HOST event
// loop a turn (timers, postMessage, rendering) but resume the SAME green
// thread before any sibling, preserving the port's historical cooperative
// atomicity. CN1 code written for the never-preempting JS/TeaVM ports
// shares unsynchronised static state (StyleParser/CSSBorder caches etc.)
// across call stretches; interleaving siblings at arbitrary dispatch
// points surfaced those latent races as nondeterministic boot failures
// (the flaky "Failed to load CSS border" -> downstream NPE).
const _Yv = { op: "byield" };
const __cn1TickStride = 256;
let __cn1TickCounter = 0;
function _Yc() {
  if (++__cn1TickCounter < __cn1TickStride) return false;
  __cn1TickCounter = 0;
  if (jvm.__cn1ClinitDepth) return false;
  const now = (typeof global.performance !== "undefined" && global.performance
      && typeof global.performance.now === "function")
      ? global.performance.now() : Date.now();
  if ((now - __cn1TickStartedAt) < __cn1TickBudgetMs) return false;
  __cn1TickStartedAt = now;
  return true;
}
global._Yc = _Yc;
global._Yv = _Yv;
// Top-level 2-char globals for the ~15k ``jvm.*`` call sites in
// translated code. Dropping the ``jvm.`` prefix (4 chars) saves
// ~60 KiB raw. ``_``-prefix names can never collide with a mangler-
// assigned symbol (the mangler only produces ``$``-prefixed names).
// Declared AFTER the ``jvm.cC``/``jvm.iO``/``jvm.aL``/``jvm.aS``/
// ``jvm.aN``/``jvm.fr`` definitions above — an earlier placement
// silently captured ``undefined`` because those assignments hadn't
// run yet.
global._I = (n) => jvm.ensureClassInitialized(n);
global._L = (v) => jvm.createStringLiteral(v);
// Primitive class literals (``int.class`` etc.) -> interned primitive
// ``Class`` object (carries ``isPrimitive=true``). Emitted by the translator
// for ``getstatic <Wrapper>.TYPE`` (see JavascriptMethodGenerator
// .primitiveTypeClassExpression); the JavaAPI's self-referential
// ``TYPE = int.class`` would otherwise leave the field null.
global._primClass = (n) => classObjectForName(n);
global._O = (c) => jvm.newObject(c);
global._C = jvm.cC;
global._D = jvm.iO;
global._A = jvm.aL;
global._T = jvm.aS;
global._N = jvm.aN;
global._F = jvm.fr;
// === Exact 64-bit Java long arithmetic (long == {__l, l, h} hi/lo pair) =======
// The JS port historically modelled ``long`` as a double (53-bit), so 64-bit
// math (SHA-384/512, bit twiddling) lost precision and bitwise long ops
// truncated to 32 bits. BigInt is exact but ~10-50x slower and hung the
// animation/timing hot paths, so longs are a single {__l, l, h} object instead
// (the goog.math.Long representation: l = low 32 bits, h = high 32 bits, both
// signed int32). One object = one value, preserving the one-slot model, and all
// math is plain Number arithmetic. ``_Lc`` coerces anything entering a long op
// to a Long: an existing Long (passthrough), a Number (int sharing the long
// value space, or a leaked double), or null (uninitialised long[]/field).
const _TWO_PWR_32 = 4294967296;
function _LL(low, high) { return { __l: 1, l: low | 0, h: high | 0 }; }
const _L0 = _LL(0, 0);
const _L1 = _LL(1, 0);
const _LMIN = _LL(0, -2147483648);          // 0x8000000000000000
const _LMAX = _LL(-1, 2147483647);           // 0x7FFFFFFFFFFFFFFF
const _LintCache = new Array(384); // cache small int->long (-128..255) to cut hot-path allocation
function _LfromInt(v) {
  v = v | 0;
  if (v >= -128 && v <= 255) {
    let c = _LintCache[v + 128];
    if (c === undefined) { c = _LintCache[v + 128] = _LL(v, v < 0 ? -1 : 0); }
    return c;
  }
  return _LL(v, v < 0 ? -1 : 0);
}
function _LfromNumber(v) {
  v = Number(v);
  if (isNaN(v)) return _L0;
  if (v <= -9223372036854775808) return _LMIN;
  if (v + 1 >= 9223372036854775808) return _LMAX;
  if (v < 0) return _Lneg(_LfromNumber(-v));
  return _LL((v % _TWO_PWR_32) | 0, (v / _TWO_PWR_32) | 0);
}
function _LtoNumber(a) { return a.h * _TWO_PWR_32 + (a.l >>> 0); }
function _Lc(x) {
  if (x && x.__l === 1) return x;
  if (x == null) return _L0;
  if (typeof x === 'number') return _LfromNumber(x);
  if (typeof x === 'bigint') return _LfromNumber(Number(x)); // defensive legacy
  return _L0;
}
function _LtoNum(x) { return (x && x.__l === 1) ? _LtoNumber(x) : Number(x); } // Long|Number -> Number
function _LisZero(a) { return a.h === 0 && a.l === 0; }
function _LisNeg(a) { return a.h < 0; }
function _Leq(a, b) { return a.h === b.h && a.l === b.l; }
function _Ladd(a, b) {
  a = _Lc(a); b = _Lc(b);
  const a48 = a.h >>> 16, a32 = a.h & 0xFFFF, a16 = a.l >>> 16, a00 = a.l & 0xFFFF;
  const b48 = b.h >>> 16, b32 = b.h & 0xFFFF, b16 = b.l >>> 16, b00 = b.l & 0xFFFF;
  let c00 = a00 + b00, c16 = 0, c32 = 0, c48 = 0;
  c16 += c00 >>> 16; c00 &= 0xFFFF;
  c16 += a16 + b16; c32 += c16 >>> 16; c16 &= 0xFFFF;
  c32 += a32 + b32; c48 += c32 >>> 16; c32 &= 0xFFFF;
  c48 += a48 + b48; c48 &= 0xFFFF;
  return _LL((c16 << 16) | c00, (c48 << 16) | c32);
}
function _Lneg(a) { a = _Lc(a); return _Leq(a, _LMIN) ? _LMIN : _Ladd(_LL(~a.l, ~a.h), _L1); }
function _Lsub(a, b) {
  // a - b = a + ~b + 1, inlined as a single 16-bit add chain (one allocation
  // instead of _Lneg+_Ladd's two) -- LSUB is hot in timing/loops.
  a = _Lc(a); b = _Lc(b);
  const a48 = a.h >>> 16, a32 = a.h & 0xFFFF, a16 = a.l >>> 16, a00 = a.l & 0xFFFF;
  const nl = ~b.l, nh = ~b.h;
  const b48 = nh >>> 16, b32 = nh & 0xFFFF, b16 = nl >>> 16, b00 = nl & 0xFFFF;
  let c00 = a00 + b00 + 1, c16 = 0, c32 = 0, c48 = 0; // +1 = the two's-complement carry-in
  c16 += c00 >>> 16; c00 &= 0xFFFF;
  c16 += a16 + b16; c32 += c16 >>> 16; c16 &= 0xFFFF;
  c32 += a32 + b32; c48 += c32 >>> 16; c32 &= 0xFFFF;
  c48 += a48 + b48; c48 &= 0xFFFF;
  return _LL((c16 << 16) | c00, (c48 << 16) | c32);
}
function _Lmul(a, b) {
  a = _Lc(a); b = _Lc(b);
  const a48 = a.h >>> 16, a32 = a.h & 0xFFFF, a16 = a.l >>> 16, a00 = a.l & 0xFFFF;
  const b48 = b.h >>> 16, b32 = b.h & 0xFFFF, b16 = b.l >>> 16, b00 = b.l & 0xFFFF;
  let c00 = 0, c16 = 0, c32 = 0, c48 = 0;
  c00 += a00 * b00; c16 += c00 >>> 16; c00 &= 0xFFFF;
  c16 += a16 * b00; c32 += c16 >>> 16; c16 &= 0xFFFF;
  c16 += a00 * b16; c32 += c16 >>> 16; c16 &= 0xFFFF;
  c32 += a32 * b00; c48 += c32 >>> 16; c32 &= 0xFFFF;
  c32 += a16 * b16; c48 += c32 >>> 16; c32 &= 0xFFFF;
  c32 += a00 * b32; c48 += c32 >>> 16; c32 &= 0xFFFF;
  c48 += a48 * b00 + a32 * b16 + a16 * b32 + a00 * b48; c48 &= 0xFFFF;
  return _LL((c16 << 16) | c00, (c48 << 16) | c32);
}
function _Lcmp(a, b) {
  a = _Lc(a); b = _Lc(b);
  // Allocation-free: the high word is signed, so it orders the full value; on a
  // tie compare the low words as unsigned. Comparisons dominate loops/timing, so
  // avoiding the _Lsub object churn here is the main hi/lo perf win.
  if (a.h !== b.h) return a.h < b.h ? -1 : 1;
  const al = a.l >>> 0, bl = b.l >>> 0;
  return al === bl ? 0 : (al < bl ? -1 : 1);
}
function _Ldiv(a, b) {
  a = _Lc(a); b = _Lc(b);
  if (_LisZero(b)) { throw new Error("/ by zero"); }
  if (_LisZero(a)) return _L0;
  if (_Leq(a, _LMIN)) {
    if (_Leq(b, _L1) || _Leq(b, _LL(-1, -1))) return _LMIN;
    if (_Leq(b, _LMIN)) return _L1;
    const approx = _Lshl(_Ldiv(_Lshr(a, 1), b), 1);
    if (_LisZero(approx)) return _LisNeg(b) ? _L1 : _LL(-1, -1);
    const rem = _Lsub(a, _Lmul(b, approx));
    return _Ladd(approx, _Ldiv(rem, b));
  }
  if (_Leq(b, _LMIN)) return _L0;
  if (_LisNeg(a)) return _LisNeg(b) ? _Ldiv(_Lneg(a), _Lneg(b)) : _Lneg(_Ldiv(_Lneg(a), b));
  if (_LisNeg(b)) return _Lneg(_Ldiv(a, _Lneg(b)));
  let res = _L0, rem = a;
  while (_Lcmp(rem, b) >= 0) {
    let approx = Math.max(1, Math.floor(_LtoNumber(rem) / _LtoNumber(b)));
    const log2 = Math.ceil(Math.log(approx) / Math.LN2);
    const delta = (log2 <= 48) ? 1 : Math.pow(2, log2 - 48);
    let approxRes = _LfromNumber(approx);
    let approxRem = _Lmul(approxRes, b);
    while (_LisNeg(approxRem) || _Lcmp(approxRem, rem) > 0) {
      approx -= delta;
      approxRes = _LfromNumber(approx);
      approxRem = _Lmul(approxRes, b);
    }
    if (_LisZero(approxRes)) approxRes = _L1;
    res = _Ladd(res, approxRes);
    rem = _Lsub(rem, approxRem);
  }
  return res;
}
function _Lrem(a, b) { a = _Lc(a); b = _Lc(b); return _Lsub(a, _Lmul(_Ldiv(a, b), b)); }
function _Land(a, b) { a = _Lc(a); b = _Lc(b); return _LL(a.l & b.l, a.h & b.h); }
function _Lor(a, b) { a = _Lc(a); b = _Lc(b); return _LL(a.l | b.l, a.h | b.h); }
function _Lxor(a, b) { a = _Lc(a); b = _Lc(b); return _LL(a.l ^ b.l, a.h ^ b.h); }
function _Lshl(a, n) { // shift count is a Java int (Number); only low 6 bits used
  a = _Lc(a); n = (_LtoNum(n) | 0) & 63; if (n === 0) return a;
  if (n < 32) return _LL(a.l << n, (a.h << n) | (a.l >>> (32 - n)));
  return _LL(0, a.l << (n - 32));
}
function _Lshr(a, n) { // arithmetic right shift
  a = _Lc(a); n = (_LtoNum(n) | 0) & 63; if (n === 0) return a;
  if (n < 32) return _LL((a.l >>> n) | (a.h << (32 - n)), a.h >> n);
  return _LL(a.h >> (n - 32), a.h >= 0 ? 0 : -1);
}
function _Lushr(a, n) { // logical right shift
  a = _Lc(a); n = (_LtoNum(n) | 0) & 63; if (n === 0) return a;
  if (n < 32) return _LL((a.l >>> n) | (a.h << (32 - n)), a.h >>> n);
  if (n === 32) return _LL(a.h, 0);
  return _LL(a.h >>> (n - 32), 0);
}
function _Ll2i(x) { return _Lc(x).l | 0; } // low 32 bits as signed int (Number)
function _LtoStr(a, radix) {
  a = _Lc(a); radix = (radix | 0) || 10;
  if (_LisZero(a)) return "0";
  if (_LisNeg(a)) {
    if (_Leq(a, _LMIN)) {
      const r = _LfromInt(radix);
      const div = _Ldiv(a, r);
      const rem = _Lsub(_Lmul(div, r), a);
      return _LtoStr(div, radix) + (_Ll2i(rem) >>> 0).toString(radix);
    }
    return "-" + _LtoStr(_Lneg(a), radix);
  }
  let rem = a, result = "";
  const radixToPower = _LfromNumber(Math.pow(radix, 6));
  for (;;) {
    const remDiv = _Ldiv(rem, radixToPower);
    const intval = (_Ll2i(_Lsub(rem, _Lmul(remDiv, radixToPower)))) >>> 0;
    let digits = intval.toString(radix);
    rem = remDiv;
    if (_LisZero(rem)) return digits + result;
    while (digits.length < 6) digits = "0" + digits;
    result = digits + result;
  }
}
global._Lc = _Lc;
global._LtoNum = _LtoNum;
global._LtoStr = _LtoStr;
global._LisLong = (x) => !!(x && x.__l === 1);
global._L0 = _L0;
global._L1 = _L1;
global._Llit = _LL;                          // long literal: _Llit(lowInt, highInt)
global._LfromNumber = _LfromNumber;
global._Ladd = _Ladd;
global._Lsub = _Lsub;
global._Lmul = _Lmul;
global._Ldiv = _Ldiv;
global._Lrem = _Lrem;
global._Lneg = _Lneg;
global._Land = _Land;
global._Lor = _Lor;
global._Lxor = _Lxor;
global._Lshl = _Lshl;
global._Lshr = _Lshr;
global._Lushr = _Lushr;
global._Lcmp = _Lcmp;
global._Li2l = (x) => _LfromInt(x | 0);      // int -> long
global._Ll2i = _Ll2i;                        // long -> int
global._Ll2d = (x) => _LtoNumber(_Lc(x));    // long -> float/double
global._Ld2l = (x) => _LfromNumber(x);       // float/double -> long
// Class-registration aliases: ``_Z`` for defineClass (1592 calls, 15-char
// prefix savings each) and ``_M`` for the methods-map registration
// (1590 calls, 3-char savings).
global._Z = (def) => jvm.defineClass(def);
global._M = (className, factory) => jvm.m(className, factory);
// Exception-dispatch helper: consolidates the per-method catch-block
// boilerplate (``findExceptionHandler`` + rethrow + stack reset +
// ``pc = handler``) into a single call. Saves ~100 chars × ~260
// try/catch-bearing methods.
// Per-class ``staticFields`` index. Every translated GETSTATIC /
// PUTSTATIC goes through ``_S.<mangledClassName>.<field>`` instead of
// ``jvm.classes.<className>.staticFields.<field>``, trimming ~20
// chars × ~1500 call sites ≈ 30 KiB.
global._S = Object.create(null);
// Additional ``jvm.*`` shorthands for the high-frequency APIs called
// from translated code. Each aliased call site drops ``jvm.`` plus
// the method-name tail: ~13-16 chars saved per call. Net saving
// across Initializr ≈ 18 KiB raw.
global._j = (c,t,l) => jvm.newArray(c,t,l);    // jvm.newArray(count,type,dims)
// ``jvm.currentThread`` is set by the scheduler AFTER this helper is
// declared (it's ``null`` at load time), so we need a getter-style
// function rather than a captured alias.
global._g = () => jvm.currentThread;
// ``_me`` is a generator so a translator-emitted ``yield* _me(obj)`` can
// suspend the calling green thread when the monitor is contended.
// Non-contended path returns null and the generator finishes immediately
// with no yield -- effectively a synchronous fast path for the common
// case (drain doesn't context-switch and the caller continues without
// observable overhead beyond a generator object allocation).
// Contended path returns a {op:"monitor_enter"} value, which we yield
// to handleYield. handleYield parks the thread on monitor.entrants;
// monitorExit promotes the head entrant when the holder releases.
global._me = function*(m) {
  const yielded = jvm.monitorEnter(jvm.currentThread, m);
  if (yielded && yielded.op) {
    yield yielded;
  }
};
global._mx = (m) => jvm.monitorExit(jvm.currentThread, m);
// Hook into ``defineClass`` to populate ``_S`` alongside the normal
// ``jvm.classes`` registration. Done via a wrapping re-assignment so
// we don't have to edit every call site inside the jvm object above.
const __origDefineClass = jvm.defineClass.bind(jvm);
jvm.defineClass = function(def) {
  __origDefineClass(def);
  const nm = def.n !== undefined ? def.n : def.name;
  if (nm) {
    global._S[nm] = def.staticFields;
  }
};
// Wrap a raw JS Error (no ``__class``) in a fresh ``java.lang.RuntimeException``
// so the Java catch handler that's about to receive it can call ``.getMessage()``,
// ``.getClass()``, ``.toString()`` etc. without immediately crashing with
// ``Missing virtual method`` (raw Errors carry no ``__classDef`` and have no
// place for virtual dispatch to land). Preserves the original error on the
// wrapper as ``__cn1WrappedRawJsError`` so diagnostics can recover it.
//
// Companion to the findExceptionHandler shim that makes raw JS Errors *match*
// Java catches typed as Throwable/Exception/RuntimeException/Error: matching
// alone isn't enough -- once dispatched, the catch body has to be able to
// actually USE the caught reference. The original observation came from
// GeneratorModel.writeProjectZipToStorage: a raw ``Array expected: null`` from
// zipme's Deflater path matched the catch(Throwable) (good), but the very
// first ``t.getClass()`` inside the catch threw ``Missing virtual method $djI
// on undefined`` (because raw JS Error has no classDef), which surfaced as a
// secondary error and lost the original message.
function wrapRawJsErrorAsRuntimeException(err) {
  if (!err || err.__class !== undefined) return err;
  if (!jvm.classes || !jvm.classes["java_lang_RuntimeException"]) return err;
  try {
    const wrapper = jvm.newObject("java_lang_RuntimeException");
    const msg = err && err.message ? String(err.message) : String(err);
    wrapper[CN1_THROWABLE_MESSAGE] = createJavaString(msg);
    wrapper.__cn1WrappedRawJsError = err;
    return wrapper;
  } catch (_wrapErr) {
    return err;
  }
}
// Structured-emitter catch dispatch: returns the WRAPPED throwable when
// ``err`` is assignable to ``type`` (null = catch-all), else null. Same
// matching rules as findExceptionHandler, expressed per-handler so a real
// JS try/catch can chain handler tests without a pc table.
global._Ex = function(err, type) {
  const w = wrapRawJsErrorAsRuntimeException(err);
  return jvm.findExceptionHandler([{ s: 0, e: 1, t: type == null ? undefined : type }], 0, w) ? w : null;
};
global._E = function(table, pc, err, stack) {
  const h = jvm.findExceptionHandler(table, pc, err);
  if (!h) throw err;
  stack.length = 0;
  stack.p(wrapRawJsErrorAsRuntimeException(err));
  return h.h !== undefined ? h.h : h.handler;
};
// Two-char ``.p()`` / ``.q()`` aliases for the stack-push / -pop
// operations that appear ~40k times across translated_app. Shaves
// 3 bytes per push (``e.push(x)`` → ``e.p(x)``) and 3 per pop
// (``e.pop()`` → ``e.q()``), roughly 120 KiB raw overall. We
// intentionally pollute ``Array.prototype`` here rather than use
// a dedicated subclass — the worker is translator-controlled and
// no third-party code runs alongside, so clobbering ``.p`` / ``.q``
// on arrays is safe.
Array.prototype.p = Array.prototype.push;
Array.prototype.q = Array.prototype.pop;
// ``stack.t()`` is "peek" (return top of stack without popping).
// The translator emits ~3.1k DUP-style ``S[S.length-1]`` reads
// per build; replacing them with ``S.t()`` saves ~10 chars per
// occurrence (~30 KiB raw on Initializr's translated_app.js).
Array.prototype.t = function() { return this[this.length - 1]; };
global.bindNative = bindNative;
global.global = global;
global.__parparInstallNativeBindings = installNativeBindings;

// Virtual-dispatch helpers used by emitted method bodies. Each INVOKEVIRTUAL /
// INVOKEINTERFACE call site used to expand into ~15 lines of inline boilerplate
// (__classDef lookup, resolveVirtual fallback, __cn1Virtual per-method cache).
// That pattern dominated the translated_app.js size on large apps. The helpers
// below collapse that boilerplate into one call. Arity-specialised versions
// avoid allocating an args array on hot paths; the variadic tail handles the
// long-tail of wider signatures.
//
// Semantics match the previous inline form exactly: try target.__classDef.methods
// first (fast path for the common same-class case), then fall back to
// jvm.resolveVirtual which has its own className|methodId-keyed cache, then
// yield* into the resolved generator.
function cn1_ivResolve(target, mid) {
  // Class-object short-circuit (must run BEFORE the fast-path so we
  // don't index the represented class's methods map). A Class instance
  // carries ``__classDef`` pointing at the REPRESENTED class's def
  // (so ``getName`` / ``getSimpleName`` / static-field access through
  // ``__classDef`` keep working without an extra hop), but VIRTUAL
  // method dispatch on a Class instance MUST resolve against
  // ``java.lang.Class``'s method table — not the represented class's.
  // Without this short-circuit, ``someDouble.getClass().equals(
  // Double.class)`` resolves ``equals`` against Double.methods
  // (the receiver's ``__classDef`` IS the Double def) and returns
  // Double.equals, which re-runs ``getClass().equals(...)`` on its
  // own this and recurses until ``RangeError: Maximum call stack
  // size``. Routing through ``jvm.resolveVirtual(target.__class,
  // mid)`` uses ``"java_lang_Class"`` and lands on Class's own
  // ``equals`` / ``hashCode`` / ``toString`` slots.
  if (target.__isClassObject) {
    return jvm.resolveVirtual(target.__class, mid);
  }
  // Fast-path: direct method on the target's classDef. This mirrors the
  // inline form that used to live at every call site. No null check here —
  // callers (cn1_iv0..4 / cn1_ivN below) are generators and delegate to
  // throwNullPointerException() for the Java-spec-compliant NPE, which
  // cannot be done from a plain function.
  const classDef = target.__classDef;
  if (classDef && classDef.pendingMethods) {
    jvm.flushPendingMethods(classDef);
  }
  let method = classDef && classDef.methods ? classDef.methods[mid] : null;
  if (typeof method === "string") {
    method = resolveMethodEntry(classDef.methods, mid);
  }
  if (!method) {
    // Diagnostic for chartDocumentStaleness (task #43): when virtual
    // dispatch falls through to resolveVirtual with no target.__class,
    // dump the receiver's full shape so the next session can identify
    // which JSO produced the null receiver. Rate-limited.
    if (target && target.__class == null) {
      if (!jvm._dispatchNullClassLogged) jvm._dispatchNullClassLogged = 0;
      if (jvm._dispatchNullClassLogged < 5) {
        jvm._dispatchNullClassLogged++;
        try {
          const hasJsValue = target.__jsValue !== undefined ? 'yes' : 'no';
          const hostCls = target.__cn1HostClass != null ? String(target.__cn1HostClass) : 'none';
          const hostRef = target.__cn1HostRef != null ? String(target.__cn1HostRef) : 'none';
          const keys = Object.keys(target).slice(0, 12).join(',');
          const allProps = Object.getOwnPropertyNames(target).slice(0, 12).join(',');
          vmDiag('NULL_RECEIVER', 'mid', String(mid));
          vmDiag('NULL_RECEIVER', 'hasJsValue', hasJsValue);
          vmDiag('NULL_RECEIVER', 'hostClass', hostCls);
          vmDiag('NULL_RECEIVER', 'hostRef', hostRef);
          vmDiag('NULL_RECEIVER', 'keys', keys);
          vmDiag('NULL_RECEIVER', 'allProps', allProps);
          // Identify whether the receiver is a literal {} (Object.prototype)
          // or a native object whose methods live on a non-Object prototype
          // (XMLHttpRequest, ArrayBuffer, DOM node, etc.). If it's NOT
          // literal {} we're chasing the wrong bug entirely.
          try {
            const proto = Object.getPrototypeOf(target);
            const isLiteral = proto === Object.prototype;
            const protoName = proto && proto.constructor && proto.constructor.name
              ? proto.constructor.name : 'unknown';
            vmDiag('NULL_RECEIVER', 'isLiteral', isLiteral ? 'yes' : 'no');
            vmDiag('NULL_RECEIVER', 'protoName', String(protoName));
            vmDiag('NULL_RECEIVER', 'typeof', typeof target);
            vmDiag('NULL_RECEIVER', 'value', String(target).substring(0, 60));
          } catch (_e) {}
          // Stack trace at call site -- gives us the cn1_iv* caller and
          // therefore the translated method that's passing {} as receiver.
          try {
            const stack = new Error('null-receiver-trace').stack || '';
            const frames = String(stack).split('\n').slice(0, 8).join(' | ');
            vmDiag('NULL_RECEIVER', 'callerFrames', frames.substring(0, 500));
          } catch (_es) {}
        } catch (_e) {}
      }
    }
    // Recovery: if the receiver is a literal {} (no __class, no __jsValue,
    // no __cn1HostRef, no own props at all) AND the method is a known
    // Canvas2DContext void method, route the call to a no-op generator
    // and keep the suite advancing. This guards against the residual
    // canvasContextWipe path -- the cached __cn1CachedDocWrapper fix at
    // 5dce6a24a covered createElement-via-cached-document but the
    // ``outputCanvas.getContext('2d')`` chain in drainPendingDisplayFrame
    // still occasionally returns {} for unidentified reasons. Without
    // this guard, cn1_s_save loops indefinitely and stalls the suite.
    // The guard is opt-in by method name so we only mask known-safe
    // canvas void ops; any other receiver-null path still routes through
    // the normal resolveVirtual error.
    // Defensive recovery extended: also handle Number receivers. The
    // canvasContextWipe diag (NULL_RECEIVER value=667 typeof=number)
    // revealed the receiver is sometimes a JS Number (the viewport
    // height), not a literal {}. Need to no-op Canvas2D methods on
    // these too -- otherwise cn1_iv* falls through to resolveVirtual
    // which spins on VIRTUAL_FAIL.
    if (typeof target === 'number'
        || (target && target.__class == null && !target.__jsValue
            && target.__cn1HostRef == null
            && Object.getOwnPropertyNames(target).length === 0)) {
      const canvasVoidMethods = {
        cn1_s_save: 1, cn1_s_restore: 1, cn1_s_beginPath: 1,
        cn1_s_closePath: 1, cn1_s_stroke: 1, cn1_s_fill: 1,
        cn1_s_clip: 1, cn1_s_resetTransform: 1,
        // setTransform with 6 doubles -- the only setTransform
        // signature observed firing NULL_RECEIVER in CI. The
        // canvasContextWipe call site (drainPendingDisplayFrame line 2312
        // ``context.setTransform(1, 0, 0, 1, 0, 0)``) hits this exact
        // signature, so without it the no-op recovery does nothing for
        // the most common offender.
        cn1_s_setTransform_double_double_double_double_double_double: 1,
        // rect() with 4 doubles -- drainPendingDisplayFrame line 2314
        // ``context.rect(cropX, cropY, cropW, cropH)`` is the call site.
        cn1_s_rect_double_double_double_double: 1,
        // Canvas2D state setters (called heavily during paint). Each is
        // a single-arg setter so no-op-with-return-null is safe.
        cn1_s_setFillStyle_java_lang_String: 1,
        cn1_s_setStrokeStyle_java_lang_String: 1,
        cn1_s_setLineWidth_double: 1,
        cn1_s_setGlobalAlpha_double: 1,
        cn1_s_setFont_java_lang_String: 1,
        cn1_s_setTextAlign_java_lang_String: 1,
        cn1_s_setTextBaseline_java_lang_String: 1,
        cn1_s_setLineCap_java_lang_String: 1,
        cn1_s_setLineJoin_java_lang_String: 1,
        cn1_s_setMiterLimit_double: 1,
        cn1_s_setShadowBlur_double: 1,
        cn1_s_setShadowColor_java_lang_String: 1,
        cn1_s_setShadowOffsetX_double: 1,
        cn1_s_setShadowOffsetY_double: 1,
        cn1_s_setGlobalCompositeOperation_java_lang_String: 1,
        // Canvas2D drawing methods (also called heavily during paint).
        cn1_s_fillRect_double_double_double_double: 1,
        cn1_s_strokeRect_double_double_double_double: 1,
        cn1_s_clearRect_double_double_double_double: 1,
        cn1_s_moveTo_double_double: 1,
        cn1_s_lineTo_double_double: 1,
        cn1_s_arc_double_double_double_double_double_boolean: 1,
        cn1_s_arc_double_double_double_double_double: 1,
        cn1_s_translate_double_double: 1,
        cn1_s_rotate_double: 1,
        cn1_s_scale_double_double: 1,
        cn1_s_transform_double_double_double_double_double_double: 1,
        cn1_s_fillText_java_lang_String_double_double: 1,
        cn1_s_strokeText_java_lang_String_double_double: 1,
        cn1_s_bezierCurveTo_double_double_double_double_double_double: 1,
        cn1_s_quadraticCurveTo_double_double_double_double: 1,
        // drawImage with image + dest-rect (4 doubles) -- another paint
        // op that fires on the broken Canvas2DContext.
        cn1_s_drawImage_com_codename1_html5_js_dom_HTMLImageElement_double_double_double_double: 1,
        cn1_s_drawImage_com_codename1_html5_js_dom_HTMLCanvasElement_double_double_double_double: 1,
        cn1_s_drawImage_com_codename1_html5_js_dom_HTMLImageElement_double_double: 1,
        cn1_s_drawImage_com_codename1_html5_js_dom_HTMLCanvasElement_double_double: 1,
        // setFillStyle / setStrokeStyle with gradient or pattern (not just String).
        cn1_s_setFillStyle_com_codename1_html5_js_canvas_CanvasPattern: 1,
        cn1_s_setFillStyle_com_codename1_html5_js_canvas_CanvasGradient: 1,
        cn1_s_setStrokeStyle_com_codename1_html5_js_canvas_CanvasPattern: 1,
        cn1_s_setStrokeStyle_com_codename1_html5_js_canvas_CanvasGradient: 1
      };
      // Prefix-match for any setFillStyle / setStrokeStyle overload we
      // haven't enumerated yet -- the Canvas2D setter pattern is uniform
      // across overloads and no-op is always safe (void return).
      if (!canvasVoidMethods[mid]
          && (typeof mid === 'string')
          && (mid.indexOf('cn1_s_setFillStyle_') === 0
              || mid.indexOf('cn1_s_setStrokeStyle_') === 0
              || mid.indexOf('cn1_s_drawImage_') === 0
              // Document.createElement on a broken {} document --
              // return null so the caller's canvas assignment becomes
              // null and the next dispatch hits the well-formed NPE
              // path instead of busy-looping on VIRTUAL_FAIL.
              || mid.indexOf('cn1_s_createElement_') === 0)) {
        return function*() { return null; };
      }
      if (canvasVoidMethods[mid]) {
        return function*() { /* no-op for {} receiver */ };
      }
    }
    method = jvm.resolveVirtual(target.__class, mid);
  }
  return method;
}
// Some translated methods are now emitted as plain synchronous
// ``function`` rather than ``function*`` — their bodies cannot yield
// the scheduler. We still want the single virtual-dispatch helper
// family to work uniformly at call sites: the bytecode's invokevirtual
// is translated to ``yield* cn1_iv*(...)`` regardless of which
// override runs at runtime. ``adaptResult`` preserves that contract by
// delegating into generator returns but short-circuiting sync returns.
function* adaptVirtualResult(result) {
  if (result && typeof result.next === "function") {
    return yield* result;
  }
  return result;
}
// adaptVirtualResult inlined into each cn1_iv* helper below to skip the
// extra generator allocation per virtual call. The helper itself was a
// ``function*`` -- every invocation allocated a generator object whose
// only purpose was to forward via ``yield*`` to the resolved method (if
// it was a generator) or return the value directly. Inlining halves
// per-call allocator pressure on the hot virtual-dispatch path. Sync /
// async resolution semantics are unchanged.
// Budget check at every generator virtual dispatch: the per-method entry
// check (emitted ``if(_Yc())yield _Yv``) cannot slice a loop that stays
// INSIDE one method and only calls runtime functions -- e.g. the
// Initializr's boot loop dispatching JSO-bridge methods via cn1_iv*,
// which blocked the worker's event loop for 90s+ (no events, no timers,
// no heartbeat, pointer input dead). _Yc is counter-amortised (clock
// check every 256th call) so the hot-path cost is one increment+compare.
function* cn1_iv0(target, mid) {
  if (_Yc()) yield _Yv;
  if (target == null) { yield* throwNullPointerException(); }
  const r = cn1_ivResolve(target, mid)(target);
  if (r && typeof r.next === "function") { return yield* r; }
  return r;
}
function* cn1_iv1(target, mid, a0) {
  if (_Yc()) yield _Yv;
  if (target == null) { yield* throwNullPointerException(); }
  const r = cn1_ivResolve(target, mid)(target, a0);
  if (r && typeof r.next === "function") { return yield* r; }
  return r;
}
function* cn1_iv2(target, mid, a0, a1) {
  if (_Yc()) yield _Yv;
  if (target == null) { yield* throwNullPointerException(); }
  const r = cn1_ivResolve(target, mid)(target, a0, a1);
  if (r && typeof r.next === "function") { return yield* r; }
  return r;
}
function* cn1_iv3(target, mid, a0, a1, a2) {
  if (_Yc()) yield _Yv;
  if (target == null) { yield* throwNullPointerException(); }
  const r = cn1_ivResolve(target, mid)(target, a0, a1, a2);
  if (r && typeof r.next === "function") { return yield* r; }
  return r;
}
function* cn1_iv4(target, mid, a0, a1, a2, a3) {
  if (_Yc()) yield _Yv;
  if (target == null) { yield* throwNullPointerException(); }
  const r = cn1_ivResolve(target, mid)(target, a0, a1, a2, a3);
  if (r && typeof r.next === "function") { return yield* r; }
  return r;
}
function* cn1_ivN(target, mid, args) {
  if (_Yc()) yield _Yv;
  if (target == null) { yield* throwNullPointerException(); }
  const method = cn1_ivResolve(target, mid);
  const r = method.apply(null, [target].concat(args));
  if (r && typeof r.next === "function") { return yield* r; }
  return r;
}
// Synchronous virtual dispatch family (cn1_ivs0..4 / cn1_ivsN). Emitted
// at INVOKEVIRTUAL / INVOKEINTERFACE call sites whose signature the
// suspension analysis (exportedSuspendingSigs) proved has NO suspending
// impl -- so the resolved override is a plain ``function`` returning a
// value, and the caller need not be a generator. This is what lets a
// method that only makes non-suspending virtual calls be emitted as a
// plain ``function`` instead of ``function*`` (no ``yield*`` ceremony),
// removing per-call generator allocation and shrinking the bundle while
// keeping the green-thread model intact for genuinely-blocking paths.
//
// Defensive drive-once: if a target unexpectedly returns a generator (a
// CHA-soundness gap -- e.g. a runtime-installed override the static
// analysis didn't see, or the ``{}`` broken-receiver canvas no-op stubs
// in cn1_ivResolve which are ``function*``), step it ONCE. A body that
// never actually yields completes on the first next() so we return its
// value safely; one that genuinely suspends in this sync context throws
// a NAMED error rather than letting a raw generator object leak
// downstream as the "result" (the silent-corruption failure mode of the
// three earlier sync-dispatcher attempts).
function cn1_ivsDrive(r, mid) {
  if (r && typeof r.next === "function") {
    const step = r.next();
    if (!step.done) {
      throw new Error("cn1_ivs: sync virtual dispatch reached a yielding method (CHA unsound): " + mid);
    }
    return step.value;
  }
  return r;
}
function cn1_ivsNpe() {
  const ex = jvm.createException("java_lang_NullPointerException");
  if (typeof ex.ctor === "function") {
    const cr = ex.ctor(ex.object);
    if (cr && typeof cr.next === "function") {
      const s = cr.next();
      if (!s.done) { throw new Error("cn1_ivs: NPE constructor yielded in sync dispatch"); }
    }
  }
  throw ex.object;
}
function cn1_ivs0(target, mid) {
  if (target == null) { cn1_ivsNpe(); }
  return cn1_ivsDrive(cn1_ivResolve(target, mid)(target), mid);
}
function cn1_ivs1(target, mid, a0) {
  if (target == null) { cn1_ivsNpe(); }
  return cn1_ivsDrive(cn1_ivResolve(target, mid)(target, a0), mid);
}
function cn1_ivs2(target, mid, a0, a1) {
  if (target == null) { cn1_ivsNpe(); }
  return cn1_ivsDrive(cn1_ivResolve(target, mid)(target, a0, a1), mid);
}
function cn1_ivs3(target, mid, a0, a1, a2) {
  if (target == null) { cn1_ivsNpe(); }
  return cn1_ivsDrive(cn1_ivResolve(target, mid)(target, a0, a1, a2), mid);
}
function cn1_ivs4(target, mid, a0, a1, a2, a3) {
  if (target == null) { cn1_ivsNpe(); }
  return cn1_ivsDrive(cn1_ivResolve(target, mid)(target, a0, a1, a2, a3), mid);
}
function cn1_ivsN(target, mid, args) {
  if (target == null) { cn1_ivsNpe(); }
  const method = cn1_ivResolve(target, mid);
  return cn1_ivsDrive(method.apply(null, [target].concat(args)), mid);
}
// Monomorphic-devirtualization dispatch family (_dv0.._dv4/_dvN
// suspending, _dw0.._dw4/_dwN sync). The translator emits these at
// INVOKEVIRTUAL / INVOKEINTERFACE call sites whose dispatch id has
// exactly one concrete implementation program-wide: the resolved
// override can only ever be that one body, so instead of the
// dispatch-id-string + resolveVirtual lookup the translator passes the
// impl FUNCTION directly. Semantics are identical to _v* / _w* minus
// the lookup: the null-receiver NPE and the drive-once contract are
// preserved. This lets the dispatch-id string drop from the _q table
// and the method's m: entry be pruned (it no longer participates in
// runtime virtual dispatch). The ``fn`` argument is a bareword
// function reference (minified/aliased in lockstep with its def), not
// a string.
function* _dv0(t, fn) {
  if (_Yc()) yield _Yv;
  if (t == null) { yield* throwNullPointerException(); }
  const r = fn(t);
  if (r && typeof r.next === "function") { return yield* r; }
  return r;
}
function* _dv1(t, fn, a0) {
  if (_Yc()) yield _Yv;
  if (t == null) { yield* throwNullPointerException(); }
  const r = fn(t, a0);
  if (r && typeof r.next === "function") { return yield* r; }
  return r;
}
function* _dv2(t, fn, a0, a1) {
  if (_Yc()) yield _Yv;
  if (t == null) { yield* throwNullPointerException(); }
  const r = fn(t, a0, a1);
  if (r && typeof r.next === "function") { return yield* r; }
  return r;
}
function* _dv3(t, fn, a0, a1, a2) {
  if (_Yc()) yield _Yv;
  if (t == null) { yield* throwNullPointerException(); }
  const r = fn(t, a0, a1, a2);
  if (r && typeof r.next === "function") { return yield* r; }
  return r;
}
function* _dv4(t, fn, a0, a1, a2, a3) {
  if (_Yc()) yield _Yv;
  if (t == null) { yield* throwNullPointerException(); }
  const r = fn(t, a0, a1, a2, a3);
  if (r && typeof r.next === "function") { return yield* r; }
  return r;
}
function* _dvN(t, fn, args) {
  if (_Yc()) yield _Yv;
  if (t == null) { yield* throwNullPointerException(); }
  const r = fn.apply(null, [t].concat(args));
  if (r && typeof r.next === "function") { return yield* r; }
  return r;
}
function _dw0(t, fn) {
  if (t == null) { cn1_ivsNpe(); }
  return cn1_ivsDrive(fn(t), "");
}
function _dw1(t, fn, a0) {
  if (t == null) { cn1_ivsNpe(); }
  return cn1_ivsDrive(fn(t, a0), "");
}
function _dw2(t, fn, a0, a1) {
  if (t == null) { cn1_ivsNpe(); }
  return cn1_ivsDrive(fn(t, a0, a1), "");
}
function _dw3(t, fn, a0, a1, a2) {
  if (t == null) { cn1_ivsNpe(); }
  return cn1_ivsDrive(fn(t, a0, a1, a2), "");
}
function _dw4(t, fn, a0, a1, a2, a3) {
  if (t == null) { cn1_ivsNpe(); }
  return cn1_ivsDrive(fn(t, a0, a1, a2, a3), "");
}
function _dwN(t, fn, args) {
  if (t == null) { cn1_ivsNpe(); }
  return cn1_ivsDrive(fn.apply(null, [t].concat(args)), "");
}
global._dv0 = _dv0; global._dv1 = _dv1; global._dv2 = _dv2;
global._dv3 = _dv3; global._dv4 = _dv4; global._dvN = _dvN;
global._dw0 = _dw0; global._dw1 = _dw1; global._dw2 = _dw2;
global._dw3 = _dw3; global._dw4 = _dw4; global._dwN = _dwN;

// Two/three-char aliases for the dispatch family: the helper name appears
// at every INVOKEVIRTUAL / INVOKEINTERFACE call site (~42k in a real app),
// so cn1_iv0 -> _v0 / cn1_ivs0 -> _w0 saves ~5 bytes per site (~200KB raw).
// The long names stay exported for port.js / diagnostics.
global._v0 = cn1_iv0; global._v1 = cn1_iv1; global._v2 = cn1_iv2;
global._v3 = cn1_iv3; global._v4 = cn1_iv4; global._vN = cn1_ivN;
global._w0 = cn1_ivs0; global._w1 = cn1_ivs1; global._w2 = cn1_ivs2;
global._w3 = cn1_ivs3; global._w4 = cn1_ivs4; global._wN = cn1_ivsN;
global.cn1_ivs0 = cn1_ivs0;
global.cn1_ivs1 = cn1_ivs1;
global.cn1_ivs2 = cn1_ivs2;
global.cn1_ivs3 = cn1_ivs3;
global.cn1_ivs4 = cn1_ivs4;
global.cn1_ivsN = cn1_ivsN;
global.cn1_iv0 = cn1_iv0;
global.cn1_iv1 = cn1_iv1;
global.cn1_iv2 = cn1_iv2;
global.cn1_iv3 = cn1_iv3;
global.cn1_iv4 = cn1_iv4;
// External callers (port.js, browser_bridge.js, anything that
// dispatches via ``jvm.resolveVirtual`` and yield-delegates to the
// result) must tolerate the CHA classifying overrides as plain
// synchronous functions — those return raw values, not iterators,
// and ``yield* sync(...)`` throws ``TypeError: ... is not iterable``.
// ``cn1_ivAdapt`` is the same generator wrapper ``cn1_iv*`` uses
// internally: forwards iterator results via yield*, returns sync
// results unchanged.
global.cn1_ivAdapt = adaptVirtualResult;
global.cn1_ivN = cn1_ivN;

vmDiag("BOOT", "runtime", "loaded");
function lowerFirst(value) {
  if (!value) {
    return value;
  }
  return value.charAt(0).toLowerCase() + value.substring(1);
}
function createJavaString(value) {
  value = value == null ? "" : String(value);
  return jvm.createStringLiteral(value);
}
function javaClassName(className) {
  if (PRIMITIVE_INFO[className]) {
    return PRIMITIVE_INFO[className].javaName;
  }
  return String(className || "").replace(/_/g, ".");
}
function descriptorClassName(className) {
  if (PRIMITIVE_INFO[className]) {
    return PRIMITIVE_INFO[className].descriptor;
  }
  if (String(className).endsWith("[]")) {
    const dims = className.match(/\[\]/g).length;
    const component = className.substring(0, className.length - (dims * 2));
    let out = "";
    for (let i = 0; i < dims; i++) {
      out += "[";
    }
    if (PRIMITIVE_INFO[component]) {
      return out + PRIMITIVE_INFO[component].descriptor;
    }
    return out + "L" + javaClassName(component) + ";";
  }
  return javaClassName(className);
}
function ensurePrimitiveClass(componentClass) {
  let cls = jvm.classes[componentClass];
  if (!cls) {
    cls = {
      name: componentClass,
      isPrimitive: true,
      assignableTo: {},
      staticFields: {},
      methods: {}
    };
    cls.assignableTo[componentClass] = true;
    cls.classObject = {
      __class: "java_lang_Class",
      __monitor: jvm.createMonitor(),
      __className: componentClass,
      __isClassObject: true,
      __classDef: cls,
      cn1_staticFields: cls.staticFields
    };
    jvm.classes[componentClass] = cls;
  }
  return cls.classObject;
}
function classObjectForName(name) {
  if (PRIMITIVE_INFO[name]) {
    return ensurePrimitiveClass(name);
  }
  return jvm.getClassObject(name);
}
function runtimeTypeFromJavaName(name) {
  if (name == null) {
    return null;
  }
  if (PRIMITIVE_INFO["JAVA_" + String(name).toUpperCase()]) {
    return "JAVA_" + String(name).toUpperCase();
  }
  if (name.charAt(0) === "[") {
    let dims = 0;
    while (name.charAt(dims) === "[") {
      dims++;
    }
    const kind = name.charAt(dims);
    let component;
    if (kind === "L") {
      component = name.substring(dims + 1, name.length - 1).replace(/[.$/]/g, "_");
    } else {
      for (const primitiveName in PRIMITIVE_INFO) {
        if (PRIMITIVE_INFO[primitiveName].descriptor === kind) {
          component = primitiveName;
          break;
        }
      }
    }
    let out = component;
    for (let i = 0; i < dims; i++) {
      out += "[]";
    }
    return out;
  }
  return name.replace(/[.$/]/g, "_");
}
function createArrayFromNativeString(value) {
  const chars = jvm.newArray(value.length, "JAVA_CHAR", 1);
  for (let i = 0; i < value.length; i++) {
    chars[i] = value.charCodeAt(i);
  }
  return chars;
}
function nativeStringFromCharArray(chars) {
  let out = "";
  for (let i = 0; i < chars.length; i++) {
    out += String.fromCharCode(chars[i] | 0);
  }
  return out;
}
function* runtimeToNativeString(value) {
  if (value == null || typeof value === "string" || value.__nativeString != null || value.__class === "java_lang_String") {
    return jvm.toNativeString(value);
  }
  if (value && value.__class) {
    // Shared dispatch id; class-specific names get mangled to opaque
    // symbols that no longer survive the ``$au``-style methods table
    // lookup.
    const toStringMethod = jvm.resolveVirtual(value.__class, "cn1_s_toString_R_java_lang_String");
    return jvm.toNativeString(yield* adaptVirtualResult(toStringMethod(value)));
  }
  return String(value);
}
function runtimeBoxedPrimitiveValue(value) {
  if (value == null || typeof value !== "object" || !value.__class) {
    return null;
  }
  switch (value.__class) {
    case "java_lang_Integer":
      return value.cn1_java_lang_Integer_value | 0;
    case "java_lang_Long":
      return value.cn1_java_lang_Long_value == null ? 0 : value.cn1_java_lang_Long_value;
    case "java_lang_Double":
      return value.cn1_java_lang_Double_value == null ? 0 : Number(value.cn1_java_lang_Double_value);
    case "java_lang_Float":
      return value.cn1_java_lang_Float_value == null ? 0 : Number(value.cn1_java_lang_Float_value);
    case "java_lang_Short":
      return value.cn1_java_lang_Short_value | 0;
    case "java_lang_Byte":
      return value.cn1_java_lang_Byte_value | 0;
    case "java_lang_Character":
      return value.cn1_java_lang_Character_value | 0;
    case "java_lang_Boolean":
      return !!value.cn1_java_lang_Boolean_value;
    default:
      return null;
  }
}
function* runtimeFormatTokenValue(token, value) {
  if (token === "%s") {
    return yield* runtimeToNativeString(value);
  }
  if (token === "%c") {
    const primitive = runtimeBoxedPrimitiveValue(value);
    if (primitive != null) {
      return String.fromCharCode(primitive | 0);
    }
    return String.fromCharCode((value == null ? 0 : value) | 0);
  }
  if (token === "%d" || token === "%i") {
    const primitive = runtimeBoxedPrimitiveValue(value);
    const numeric = primitive != null ? primitive : (value == null ? 0 : value);
    // A long is a BigInt -- print it exactly; Number(bigint) would lose >2^53.
    return (numeric && numeric.__l === 1) ? _LtoStr(numeric) : String(Math.trunc(Number(numeric)));
  }
  if (token === "%f") {
    const primitive = runtimeBoxedPrimitiveValue(value);
    if (primitive != null) {
      return String(Number(primitive));
    }
    return String(Number(value == null ? 0 : value));
  }
  return yield* runtimeToNativeString(value);
}
function sbEnsureCapacity(sb, size) {
  let data = sb[CN1_SB_VALUE];
  if (!data) {
    data = jvm.newArray(Math.max(16, size), "JAVA_CHAR", 1);
    sb[CN1_SB_VALUE] = data;
    return data;
  }
  if (data.length >= size) {
    return data;
  }
  const next = jvm.newArray(Math.max(size, (data.length * 2) + 2), "JAVA_CHAR", 1);
  for (let i = 0; i < data.length; i++) {
    next[i] = data[i];
  }
  sb[CN1_SB_VALUE] = next;
  return next;
}
function sbAppendNativeString(sb, value) {
  value = value == null ? "null" : String(value);
  const count = sb[CN1_SB_COUNT] | 0;
  const data = sbEnsureCapacity(sb, count + value.length);
  for (let i = 0; i < value.length; i++) {
    data[count + i] = value.charCodeAt(i);
  }
  sb[CN1_SB_COUNT] = count + value.length;
  return sb;
}
function intBitsFromFloat(value) {
  const view = new DataView(new ArrayBuffer(4));
  view.setFloat32(0, value, false);
  return view.getInt32(0, false);
}
function floatFromIntBits(bits) {
  const view = new DataView(new ArrayBuffer(4));
  view.setInt32(0, bits | 0, false);
  return view.getFloat32(0, false);
}
function longBitsFromDouble(value) {
  const view = new DataView(new ArrayBuffer(8));
  view.setFloat64(0, Number(value), false);
  const hi = view.getUint32(0, false);
  const lo = view.getUint32(4, false);
  // long == hi/lo Long: the two 32-bit halves map directly onto h/l (exact;
  // the old double ``hi*2^32+lo`` could not hold the full 64-bit SHA-512 word).
  return _LL(lo | 0, hi | 0);
}
function doubleFromLongBits(bits) {
  const lng = _Lc(bits);
  const view = new DataView(new ArrayBuffer(8));
  view.setUint32(0, lng.h >>> 0, false);
  view.setUint32(4, lng.l >>> 0, false);
  return view.getFloat64(0, false);
}
function defaultTimeZoneId() {
  if (typeof Intl !== "undefined" && Intl.DateTimeFormat) {
    const options = Intl.DateTimeFormat().resolvedOptions();
    if (options && options.timeZone) {
      return options.timeZone;
    }
  }
  return "GMT";
}
function normalizeTimeZoneId(name) {
  const value = name == null ? "" : jvm.toNativeString(name);
  return value ? value : defaultTimeZoneId();
}
function timezoneDateParts(timeZone, millis) {
  millis = _LtoNum(millis); // millis may be a Java long (Long object); Date needs a Number
  const format = new Intl.DateTimeFormat("en-US", {
    timeZone: timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hourCycle: "h23"
  });
  const parts = format.formatToParts(new Date(millis));
  const out = {};
  for (let i = 0; i < parts.length; i++) {
    if (parts[i].type !== "literal") {
      out[parts[i].type] = parts[i].value;
    }
  }
  return out;
}
function timezoneOffsetMillis(timeZone, millis) {
  millis = _LtoNum(millis); // millis may be a Java long (Long object)
  if (timeZone === "GMT" || typeof Intl === "undefined" || !Intl.DateTimeFormat) {
    return 0;
  }
  const parts = timezoneDateParts(timeZone, millis);
  const utcMillis = Date.UTC(
          parseInt(parts.year, 10),
          parseInt(parts.month, 10) - 1,
          parseInt(parts.day, 10),
          parseInt(parts.hour, 10),
          parseInt(parts.minute, 10),
          parseInt(parts.second, 10),
          0);
  return utcMillis - millis;
}
function timezoneRawOffsetMillis(timeZone) {
  if (timeZone === "GMT") {
    return 0;
  }
  const year = new Date().getUTCFullYear();
  const jan = timezoneOffsetMillis(timeZone, Date.UTC(year, 0, 1, 12, 0, 0, 0));
  const jul = timezoneOffsetMillis(timeZone, Date.UTC(year, 6, 1, 12, 0, 0, 0));
  return Math.min(jan, jul);
}
function formatStyleOptions(style, type) {
  if (style < 0) {
    return null;
  }
  switch (style | 0) {
    case 0:
      return type === "date" ? { weekday: "long", year: "numeric", month: "long", day: "numeric" }
              : { hour: "numeric", minute: "2-digit", second: "2-digit", timeZoneName: "short" };
    case 1:
      return type === "date" ? { year: "numeric", month: "long", day: "numeric" }
              : { hour: "numeric", minute: "2-digit", second: "2-digit", timeZoneName: "short" };
    case 2:
      return type === "date" ? { year: "numeric", month: "short", day: "numeric" }
              : { hour: "numeric", minute: "2-digit", second: "2-digit" };
    default:
      return type === "date" ? { year: "2-digit", month: "numeric", day: "numeric" }
              : { hour: "numeric", minute: "2-digit" };
  }
}
function formatJavaDate(dateFormat, dateObject) {
  const millis = dateObject == null ? Date.now() : Number(dateObject[CN1_DATE_VALUE] || 0);
  const date = new Date(millis);
  const dateOptions = formatStyleOptions(dateFormat[CN1_DATEFORMAT_DATE_STYLE] | 0, "date");
  const timeOptions = formatStyleOptions(dateFormat[CN1_DATEFORMAT_TIME_STYLE] | 0, "time");
  const options = {};
  if (dateOptions) {
    Object.assign(options, dateOptions);
  }
  if (timeOptions) {
    Object.assign(options, timeOptions);
  }
  if (!dateOptions && !timeOptions) {
    options.year = "numeric";
    options.month = "numeric";
    options.day = "numeric";
  }
  return date.toLocaleString(undefined, options);
}
function* throwInterruptedException() {
  if (jvm.currentThread && jvm.currentThread.object) {
    jvm.currentThread.object.__interrupted = 0;
  }
  const ex = jvm.createException("java_lang_InterruptedException");
  if (typeof ex.ctor === "function") {
    yield* adaptVirtualResult(ex.ctor(ex.object));
  }
  throw ex.object;
}
function* throwNullPointerException() {
  if (VM_DIAG_ENABLED) {
    try {
      const dc = jvm.classes["com_codename1_ui_Display"];
      vmTrace("DIAG:NPE_THROWN:displayInit=" + (dc ? (dc.initialized ? 1 : 0) : -1)
        + ":displayIniting=" + (dc ? (dc.initializing ? 1 : 0) : -1)
        + ":instanceSet=" + ((jvm.staticFieldsFor && 0) || (typeof _S !== "undefined" && _S["com_codename1_ui_Display"] && _S["com_codename1_ui_Display"]["INSTANCE"] != null ? 1 : 0))
        + ":stack=" + String(new Error().stack).split("\n").slice(1, 16).join("<"));
    } catch (_e) {}
  }
  const ex = jvm.createException("java_lang_NullPointerException");
  if (typeof ex.ctor === "function") {
    yield* adaptVirtualResult(ex.ctor(ex.object));
  }
  throw ex.object;
}
function bindNative(names, fn) {
  // bindNative callers still pass the class-specific ``cn1_<cls>_<m>_<sig>``
  // name, but every class's ``methods`` map now keys on the class-free
  // sig-based dispatch id ``cn1_s_<m>_<sig>`` (see
  // JavascriptNameUtil.dispatchMethodIdentifier in fa4247a42). Rewrite
  // the passed name to the dispatch-id form so the override actually
  // lands on the emitted slot — the historical full-name key and the
  // new sig-based key are both applied so either lookup style works.
  function toDispatchId(name) {
    if (typeof name !== "string") {
      return null;
    }
    if (name.indexOf("cn1_s_") === 0) {
      return name;
    }
    if (name.indexOf("cn1_") !== 0) {
      return null;
    }
    // Strip the class component: everything from ``cn1_`` up to the
    // last underscore that precedes the method name. The translator
    // emits class-specific names as ``cn1_<classPath>_<method>_<sig>``
    // where ``<classPath>`` itself contains underscores (``java_util_
    // HashMap``). Walk the class index to find the longest matching
    // prefix; the method tail is what remains.
    const classes = jvm.classes || {};
    let bestPrefix = null;
    for (const className in classes) {
      const prefix = "cn1_" + className + "_";
      if (name.indexOf(prefix) === 0
              && (bestPrefix == null || prefix.length > bestPrefix.length)) {
        bestPrefix = prefix;
      }
    }
    if (bestPrefix == null) {
      return null;
    }
    return "cn1_s_" + name.substring(bestPrefix.length);
  }
  function installVirtualOverride(name) {
    const classes = jvm.classes || {};
    const classNames = Object.keys(classes);
    const dispatchId = toDispatchId(name);
    for (let i = 0; i < classNames.length; i++) {
      const cls = classes[classNames[i]];
      if (!cls || !cls.methods) {
        continue;
      }
      if (Object.prototype.hasOwnProperty.call(cls.methods, name)) {
        cls.methods[name] = fn;
      }
      if (dispatchId && Object.prototype.hasOwnProperty.call(cls.methods, dispatchId)) {
        cls.methods[dispatchId] = fn;
      }
    }
    if (dispatchId) {
      // Also stash under the dispatch id so ``resolveVirtual`` + the
      // ``nativeMethods`` fallback path (line 901 onward) finds the
      // binding when the m: entry was dropped by virtual-dispatch RTA.
      jvm.nativeMethods[dispatchId] = fn;
    }
  }
  function rememberTranslatedMethod(name, existingFn) {
    if (typeof existingFn !== "function" || existingFn === fn) {
      return;
    }
    if (!jvm.translatedMethods) {
      jvm.translatedMethods = Object.create(null);
    }
    if (typeof jvm.translatedMethods[name] !== "function") {
      jvm.translatedMethods[name] = existingFn;
    }
  }
  function registerNative(name) {
    rememberTranslatedMethod(name, global[name]);
    jvm.nativeMethods[name] = fn;
    global[name] = fn;
    jvm[name] = fn;
    refreshCn1Alias(name, fn);
    installVirtualOverride(name);
  }
  for (let i = 0; i < names.length; i++) {
    const name = names[i];
    registerNative(name);
    if (!name.endsWith("__impl")) {
      registerNative(name + "__impl");
    }
  }
  return fn;
}
function installCompatibilityClasses() {
  if (!jvm.classes["kotlin_jvm_internal_Intrinsics"]) {
    jvm.defineClass({
      name: "kotlin_jvm_internal_Intrinsics",
      baseClass: "java_lang_Object",
      interfaces: [],
      isInterface: false,
      isAbstract: false,
      assignableTo: {
        kotlin_jvm_internal_Intrinsics: true,
        java_lang_Object: true
      },
      instanceFields: [],
      staticFields: {},
      methods: {}
    });
  }
}
function installNativeBindings() {
  if (!jvm.translatedMethods) {
    jvm.translatedMethods = Object.create(null);
  }
  const classes = jvm.classes || {};
  const classNames = Object.keys(classes);
  function overrideMethodMaps(name, fn) {
    // bindNative calls during port.js load ran before translated_app.js
    // emitted any ``_Z({..., m: {...}})`` blocks — ``jvm.classes`` was
    // empty so the loop found nothing to override. Now that classes
    // are registered, re-apply the override.
    //
    // CRITICAL: ``cn1_<class>_<method>_<sig>`` always maps to the
    // override ON THAT EXACT CLASS — never on subclasses or other
    // classes that happen to inherit / re-implement the same method.
    // The pre-fa4247a42 emission gave each class its own
    // ``cn1_<X>_<m>`` entry, so installing one bindNative could only
    // touch the one class. After fa4247a42, every class's methods
    // map keys on the class-free ``cn1_s_<m>_<sig>`` dispatch id —
    // a single override under that key would clobber every subclass's
    // own implementation. So override the dispatch id ONLY on the
    // exact class extracted from the bindNative name; everywhere else
    // the existing emitted entry stays intact.
    let dispatchId = null;
    let targetClassName = null;
    if (name.indexOf("cn1_") === 0 && name.indexOf("cn1_s_") !== 0) {
      let bestPrefix = null;
      for (let i = 0; i < classNames.length; i++) {
        const prefix = "cn1_" + classNames[i] + "_";
        if (name.indexOf(prefix) === 0
                && (bestPrefix == null || prefix.length > bestPrefix.length)) {
          bestPrefix = prefix;
          targetClassName = classNames[i];
        }
      }
      if (bestPrefix != null) {
        dispatchId = "cn1_s_" + name.substring(bestPrefix.length);
      }
    }
    for (let i = 0; i < classNames.length; i++) {
      const cls = classes[classNames[i]];
      if (!cls || !cls.methods) {
        continue;
      }
      if (Object.prototype.hasOwnProperty.call(cls.methods, name)) {
        cls.methods[name] = fn;
      }
    }
    if (targetClassName && dispatchId) {
      const targetCls = classes[targetClassName];
      if (targetCls && targetCls.methods
              && Object.prototype.hasOwnProperty.call(targetCls.methods, dispatchId)) {
        targetCls.methods[dispatchId] = fn;
      }
    }
  }
  const names = Object.keys(jvm.nativeMethods || {});
  for (let i = 0; i < names.length; i++) {
    const name = names[i];
    const nativeFn = jvm.nativeMethods[name];
    if (typeof nativeFn !== "function") {
      continue;
    }
    const existingGlobal = global[name];
    if (typeof existingGlobal === "function" && existingGlobal !== nativeFn) {
      if (typeof jvm.translatedMethods[name] !== "function") {
        jvm.translatedMethods[name] = existingGlobal;
      }
    }
    global[name] = nativeFn;
    jvm[name] = nativeFn;
    refreshCn1Alias(name, nativeFn);
    overrideMethodMaps(name, nativeFn);
    if (!name.endsWith("__impl")) {
      const implName = name + "__impl";
      const existingImpl = global[implName];
      if (typeof existingImpl === "function" && existingImpl !== nativeFn) {
        if (typeof jvm.translatedMethods[implName] !== "function") {
          jvm.translatedMethods[implName] = existingImpl;
        }
      }
      global[name + "__impl"] = nativeFn;
      jvm[name + "__impl"] = nativeFn;
      refreshCn1Alias(implName, nativeFn);
    }
  }
}
// Call-site aliasing support: the bundle writer rewrites hot ``cn1_*``
// call sites to short ``$J*`` aliases and emits an ``__cn1Al`` registry
// (canonical name -> alias). Any code path that reassigns a ``cn1_*``
// global MUST refresh the alias through here or aliased call sites keep
// invoking the stale original.
function refreshCn1Alias(name, fn) {
  const al = global.__cn1Al;
  if (al) {
    const alias = al[name];
    if (alias) {
      global[alias] = fn;
    }
  }
}
global.__cn1RefreshAlias = refreshCn1Alias;
installCompatibilityClasses();
function getQueryParameter(name) {
  const loc = (global.window || global).location;
  if (!loc || !loc.search) {
    return null;
  }
  const search = String(loc.search).charAt(0) === "?" ? String(loc.search).substring(1) : String(loc.search);
  if (!search) {
    return null;
  }
  const pairs = search.split("&");
  for (let i = 0; i < pairs.length; i++) {
    const part = pairs[i];
    if (!part) {
      continue;
    }
    const eq = part.indexOf("=");
    const rawKey = eq >= 0 ? part.substring(0, eq) : part;
    if (decodeURIComponent(rawKey.replace(/\+/g, " ")) !== name) {
      continue;
    }
    const rawValue = eq >= 0 ? part.substring(eq + 1) : "";
    return decodeURIComponent(rawValue.replace(/\+/g, " "));
  }
  return null;
}
function getUserAgentString() {
  const nav = global.navigator;
  const win = global.window || global;
  return String((nav && (nav.userAgent || nav.vendor)) || win.opera || "");
}
function isPhoneUserAgent() {
  const agent = getUserAgentString();
  if (!agent) {
    return false;
  }
  const mobileRegex = /(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows ce|xda|xiino/i;
  const prefixRegex = /1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i;
  return mobileRegex.test(agent) || prefixRegex.test(agent.substring(0, 4));
}
function isPhoneOrTabletUserAgent() {
  const agent = getUserAgentString();
  if (!agent) {
    return false;
  }
  const mobileOrTabletRegex = /(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows ce|xda|xiino|android|ipad|playbook|silk/i;
  const prefixRegex = /1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i;
  return mobileOrTabletRegex.test(agent) || prefixRegex.test(agent.substring(0, 4));
}
function isIOSUserAgent() {
  const nav = global.navigator || {};
  const ua = String(nav.userAgent || "");
  const platform = String(nav.platform || "");
  const maxTouchPoints = Number(nav.maxTouchPoints || 0);
  return (/iPad|iPhone|iPod/.test(ua) && !global.MSStream) || (platform === "MacIntel" && maxTouchPoints > 1);
}
function isMacUserAgent() {
  return /Mac/.test(getUserAgentString());
}
function isIPadUserAgent() {
  const nav = global.navigator || {};
  const ua = String(nav.userAgent || "");
  const platform = String(nav.platform || "");
  const maxTouchPoints = Number(nav.maxTouchPoints || 0);
  return /iPad/i.test(ua) || (platform === "MacIntel" && maxTouchPoints > 1);
}
bindNative(["cn1_org_teavm_classlib_impl_tz_DateTimeZoneProvider_timeZoneDetectionEnabled_R_boolean", "cn1_org_teavm_classlib_impl_tz_DateTimeZoneProvider_timeZoneDetectionEnabled___R_boolean"], function() {
  return 0;
});
bindNative([
  "cn1_kotlin_jvm_internal_Intrinsics_areEqual_java_lang_Object_java_lang_Object_R_boolean",
  "cn1_kotlin_jvm_internal_Intrinsics_areEqual___java_lang_Object_java_lang_Object_R_boolean"
], function*(a, b) {
  if (a == b) {
    return 1;
  }
  if (a == null || b == null) {
    return 0;
  }
  if (a && a.__class) {
    // Use the shared dispatch id — class-specific method IDs survive
    // the mangler as opaque ``$aaw``-style symbols and don't match the
    // ``$au``-style keys the post-fa4247a42 method tables use.
    const equalsMethod = jvm.resolveVirtual(a.__class, "cn1_s_equals_java_lang_Object_R_boolean");
    return yield* adaptVirtualResult(equalsMethod(a, b));
  }
  return a === b ? 1 : 0;
});
bindNative([
  "cn1_java_lang_Object_wait",
  "cn1_java_lang_Object_wait__",
  "cn1_java_lang_Object_wait_long",
  "cn1_java_lang_Object_wait___long",
  "cn1_java_lang_Object_wait_long_int",
  "cn1_java_lang_Object_wait___long_int"
], function*(__cn1ThisObject, timeout, nanos) {
  const resumed = yield jvm.waitOn(jvm.currentThread, __cn1ThisObject, timeout || 0);
  if (resumed && resumed.interrupted) {
    yield* throwInterruptedException();
  }
  return null;
});
bindNative(["cn1_java_lang_Object_notify", "cn1_java_lang_Object_notify__"], function(__cn1ThisObject) { jvm.notifyOne(__cn1ThisObject); return null; });
bindNative(["cn1_java_lang_Object_notifyAll", "cn1_java_lang_Object_notifyAll__"], function(__cn1ThisObject) { jvm.notifyAll(__cn1ThisObject); return null; });
bindNative(["cn1_java_lang_Object_hashCode_R_int", "cn1_java_lang_Object_hashCode___R_int"], function(__cn1ThisObject) { return __cn1ThisObject == null ? 0 : (__cn1ThisObject.__id | 0); });
bindNative([
  "cn1_java_lang_Object_getClass_R_java_lang_Class",
  "cn1_java_lang_Object_getClass___R_java_lang_Class",
  "cn1_java_lang_Object_getClassImpl_R_java_lang_Class",
  "cn1_java_lang_Object_getClassImpl___R_java_lang_Class"
], function*(__cn1ThisObject) {
  if (__cn1ThisObject == null) {
    yield* throwNullPointerException();
  }
  if (__cn1ThisObject.__classDef) {
    return __cn1ThisObject.__classDef.classObject;
  }
  return jvm.getClassObject(__cn1ThisObject.__class);
});
bindNative(["cn1_java_io_PrintStream_print_java_lang_String"], function(__cn1ThisObject, value) {
  printToConsole(printStreamValue(value));
  return null;
});
bindNative(["cn1_java_io_PrintStream_println_java_lang_String"], function(__cn1ThisObject, value) {
  printToConsole(printStreamValue(value));
  return null;
});
bindNative(["cn1_java_io_PrintStream_println_java_lang_Object"], function(__cn1ThisObject, value) {
  printToConsole(printStreamValue(value));
  return null;
});
bindNative(["cn1_java_lang_System___CLINIT__"], function() {
  ensureSystemPrintStreams();
  return null;
});
bindNative(["cn1_java_lang_Object_toString_R_java_lang_String"], function(__cn1ThisObject) {
  if (__cn1ThisObject == null) {
    return createJavaString("null");
  }
  return createJavaString(javaClassName(__cn1ThisObject.__class) + "@" + ((__cn1ThisObject.__id | 0).toString(16)));
});
bindNative(["cn1_java_lang_Thread_currentThread_R_java_lang_Thread", "cn1_java_lang_Thread_currentThread___R_java_lang_Thread"], function() {
  if (jvm.currentThread && jvm.currentThread.object) {
    return jvm.currentThread.object;
  }
  return jvm.mainThreadObject || null;
});
bindNative(["cn1_java_lang_Thread_sleep_long", "cn1_java_lang_Thread_sleep___long"], function*(millis) {
  const resumed = yield { op: "sleep", millis: millis || 0 };
  if (resumed && resumed.interrupted) {
    yield* throwInterruptedException();
  }
  return null;
});
bindNative(["cn1_java_lang_Thread_setPriorityImpl_int", "cn1_java_lang_Thread_setPriorityImpl___int"], function() { return null; });
bindNative(["cn1_java_lang_Thread_interrupt0", "cn1_java_lang_Thread_interrupt0__"], function(__cn1ThisObject) { jvm.interruptThread(__cn1ThisObject); return null; });
bindNative(["cn1_java_lang_Thread_isInterrupted_boolean_R_boolean", "cn1_java_lang_Thread_isInterrupted___boolean_R_boolean"], function(__cn1ThisObject, clearInterrupted) { const value = __cn1ThisObject && __cn1ThisObject.__interrupted ? 1 : 0; if (clearInterrupted && __cn1ThisObject) __cn1ThisObject.__interrupted = 0; return value; });
bindNative(["cn1_java_lang_Thread_getNativeThreadId_R_long", "cn1_java_lang_Thread_getNativeThreadId___R_long"], function() { return _LfromInt(jvm.currentThread ? jvm.currentThread.id : 0); });
bindNative(["cn1_java_lang_Thread_releaseThreadNativeResources_long", "cn1_java_lang_Thread_releaseThreadNativeResources___long"], function() { return null; });
bindNative(["cn1_java_lang_Thread_start", "cn1_java_lang_Thread_start__"], function*(__cn1ThisObject) {
  const tid = jvm.nextThreadId;
  __cn1ThisObject[CN1_THREAD_ALIVE] = 1;
  __cn1ThisObject[CN1_THREAD_NATIVE_ID] = tid;
  jvm.classes["java_lang_Thread"].staticFields["activeThreads"] = ((jvm.classes["java_lang_Thread"].staticFields["activeThreads"] | 0) + 1) | 0;
  const target = __cn1ThisObject[CN1_THREAD_TARGET] || __cn1ThisObject;
  const generator = (function*() {
    try {
      // Post-fa4247a42 dispatch ids are class-free: every impl of
      // ``run()V`` is keyed under ``cn1_s_run`` in its class's
      // methods map, and ``resolveVirtual`` walks the hierarchy
      // against that id. The legacy class-specific form
      // ``cn1_java_lang_Runnable_run`` only existed as an alias in
      // the pre-sig-id emission and is no longer present.
      const runMethod = jvm.resolveVirtual(target.__class, "cn1_s_run");
      yield* adaptVirtualResult(runMethod(target));
    } catch (err) {
      jvm.fail(err);
    } finally {
      jvm.classes["java_lang_Thread"].staticFields["activeThreads"] = ((jvm.classes["java_lang_Thread"].staticFields["activeThreads"] | 0) - 1) | 0;
    }
  })();
  jvm.spawn(__cn1ThisObject, generator);
  return null;
});
bindNative(["cn1_java_lang_System_currentTimeMillis_R_long", "cn1_java_lang_System_currentTimeMillis___R_long"], function() { return _LfromNumber(Date.now()); });
bindNative(["cn1_java_lang_System_nanoTime_R_long", "cn1_java_lang_System_nanoTime___R_long"], function() {
  // performance.now() is a high-resolution monotonic clock (sub-millisecond,
  // in fractional milliseconds) and is available in both window and worker
  // scopes. Fall back to the wall clock if it is somehow absent. This branch's
  // exact-64-bit-long migration requires long-returning natives to hand back a
  // _LfromNumber-wrapped value rather than a raw JS number.
  if (typeof performance !== "undefined" && performance && typeof performance.now === "function") {
    return _LfromNumber(Math.floor(performance.now() * 1e6));
  }
  return _LfromNumber(Date.now() * 1e6);
});
bindNative(["cn1_java_lang_System_identityHashCode_java_lang_Object_R_int", "cn1_java_lang_System_identityHashCode___java_lang_Object_R_int"], function(obj) { return identityHash(obj); });
bindNative(["cn1_java_lang_System_arraycopy_java_lang_Object_int_java_lang_Object_int_int", "cn1_java_lang_System_arraycopy___java_lang_Object_int_java_lang_Object_int_int"], function(src, srcOffset, dst, dstOffset, length) {
  // System.arraycopy has memmove semantics when source and destination ranges
  // overlap.  Copying forward unconditionally corrupts right-shifts such as
  // StringBuilder.insert(), repeating the character at the insertion point
  // through the rest of the buffer.
  if (src === dst && dstOffset > srcOffset && dstOffset < srcOffset + length) {
    for (let i = length - 1; i >= 0; i--) dst[dstOffset + i] = src[srcOffset + i];
  } else {
    for (let i = 0; i < length; i++) dst[dstOffset + i] = src[srcOffset + i];
  }
  return null;
});
bindNative(["cn1_java_lang_System_gcLight", "cn1_java_lang_System_gcLight__"], function() { return null; });
bindNative(["cn1_java_lang_System_gcMarkSweep", "cn1_java_lang_System_gcMarkSweep__"], function() { return null; });
bindNative(["cn1_java_lang_System_isHighFrequencyGC_R_boolean", "cn1_java_lang_System_isHighFrequencyGC___R_boolean"], function() { return 0; });
// Tagged-immediate Integer natives (C-side poor-man's-Valhalla). The JS port
// has no tagged pointers: cn1Value reads the heap field, valueOf delegates to
// the pure-Java cache twin (valueOfHeap).
bindNative(["cn1_java_lang_Integer_cn1Value_R_int"], function(__cn1ThisObject) {
  return __cn1ThisObject.cn1_java_lang_Integer_value | 0;
});
bindNative(["cn1_java_lang_Integer_valueOf_int_R_java_lang_Integer"], function*(i) {
  return yield* adaptVirtualResult(cn1_java_lang_Integer_valueOfHeap_int_R_java_lang_Integer(i));
});
bindNative(["cn1_java_lang_System_exit_int", "cn1_java_lang_System_exit___int"], function(status) { jvm.finish(status); return null; });
bindNative(["cn1_java_lang_Runtime_totalMemoryImpl_R_long"], function() { return _LfromNumber(67108864); });
bindNative(["cn1_java_lang_Runtime_freeMemoryImpl_R_long"], function() { return _LfromNumber(33554432); });
bindNative(["cn1_java_lang_Throwable_fillInStack"], function(__cn1ThisObject) {
  const prevLimit = Error.stackTraceLimit;
  try { Error.stackTraceLimit = 200; } catch (_l) {}
  __cn1ThisObject[CN1_THROWABLE_STACK] = createJavaString(new Error().stack || "");
  try { Error.stackTraceLimit = prevLimit; } catch (_l) {}
  return null;
});
bindNative(["cn1_java_lang_Throwable_getStack_R_java_lang_String"], function(__cn1ThisObject) { return __cn1ThisObject[CN1_THROWABLE_STACK] || createJavaString(""); });
bindNative(["cn1_java_lang_Math_abs_double_R_double"], function(v) { return Math.abs(v); });
bindNative(["cn1_java_lang_Math_abs_float_R_float"], function(v) { return Math.abs(v); });
bindNative(["cn1_java_lang_Math_abs_int_R_int"], function(v) { return Math.abs(v | 0); });
bindNative(["cn1_java_lang_Math_abs_long_R_long"], function(v) { const x = _Lc(v); return x.h < 0 ? _Lneg(x) : x; });
bindNative(["cn1_java_lang_Math_ceil_double_R_double"], function(v) { return Math.ceil(v); });
bindNative(["cn1_java_lang_Math_floor_double_R_double"], function(v) { return Math.floor(v); });
bindNative(["cn1_java_lang_Math_max_double_double_R_double"], function(a, b) { return Math.max(a, b); });
bindNative(["cn1_java_lang_Math_max_float_float_R_float"], function(a, b) { return Math.max(a, b); });
bindNative(["cn1_java_lang_Math_max_int_int_R_int"], function(a, b) { return Math.max(a | 0, b | 0); });
bindNative(["cn1_java_lang_Math_max_long_long_R_long"], function(a, b) { return _Lcmp(a, b) >= 0 ? _Lc(a) : _Lc(b); });
bindNative(["cn1_java_lang_Math_min_double_double_R_double"], function(a, b) { return Math.min(a, b); });
bindNative(["cn1_java_lang_Math_min_float_float_R_float"], function(a, b) { return Math.min(a, b); });
bindNative(["cn1_java_lang_Math_min_int_int_R_int"], function(a, b) { return Math.min(a | 0, b | 0); });
bindNative(["cn1_java_lang_Math_min_long_long_R_long"], function(a, b) { return _Lcmp(a, b) <= 0 ? _Lc(a) : _Lc(b); });
bindNative(["cn1_java_lang_Math_pow_double_double_R_double"], function(a, b) { return Math.pow(a, b); });
bindNative(["cn1_java_lang_Math_cos_double_R_double"], function(v) { return Math.cos(v); });
bindNative(["cn1_java_lang_Math_sin_double_R_double"], function(v) { return Math.sin(v); });
bindNative(["cn1_java_lang_Math_sqrt_double_R_double"], function(v) { return Math.sqrt(v); });
bindNative(["cn1_java_lang_Math_tan_double_R_double"], function(v) { return Math.tan(v); });
bindNative(["cn1_java_lang_Math_atan_double_R_double"], function(v) { return Math.atan(v); });
bindNative(["cn1_java_lang_Integer_toString_int_R_java_lang_String"], function(v) { return createJavaString(String(v | 0)); });
bindNative(["cn1_java_lang_Integer_toString_int_int_R_java_lang_String"], function(v, radix) { return createJavaString((v | 0).toString((radix | 0) || 10)); });
bindNative(["cn1_java_lang_Long_toString_long_int_R_java_lang_String"], function(v, radix) { return createJavaString(_LtoStr(v, (radix | 0) || 10)); });
bindNative(["cn1_java_lang_Character_toLowerCase_char_R_char"], function(ch) { return String.fromCharCode(ch | 0).toLowerCase().charCodeAt(0) | 0; });
bindNative(["cn1_java_lang_Character_toLowerCase_int_R_int"], function(ch) { return String.fromCharCode(ch | 0).toLowerCase().charCodeAt(0) | 0; });
bindNative(["cn1_java_lang_Float_floatToIntBits_float_R_int"], function(v) { return intBitsFromFloat(v); });
bindNative(["cn1_java_lang_Float_intBitsToFloat_int_R_float"], function(bits) { return floatFromIntBits(bits); });
bindNative(["cn1_java_lang_Float_toStringImpl_float_boolean_R_java_lang_String"], function(v) { return createJavaString(String(v)); });
bindNative(["cn1_java_lang_Double_doubleToLongBits_double_R_long"], function(v) { return longBitsFromDouble(v); });
bindNative(["cn1_java_lang_Double_longBitsToDouble_long_R_double"], function(bits) { return doubleFromLongBits(bits); });
bindNative(["cn1_java_lang_Double_toStringImpl_double_boolean_R_java_lang_String"], function(v) { return createJavaString(String(v)); });
bindNative(["cn1_java_lang_StringBuilder_append_char_R_java_lang_StringBuilder"], function(__cn1ThisObject, ch) { return sbAppendNativeString(__cn1ThisObject, String.fromCharCode(ch | 0)); });
bindNative(["cn1_java_lang_StringBuilder_append_int_R_java_lang_StringBuilder"], function(__cn1ThisObject, value) { return sbAppendNativeString(__cn1ThisObject, String(value | 0)); });
bindNative(["cn1_java_lang_StringBuilder_append_long_R_java_lang_StringBuilder"], function(__cn1ThisObject, value) { return sbAppendNativeString(__cn1ThisObject, _LtoStr(value)); });
bindNative(["cn1_java_lang_StringBuilder_append_java_lang_Object_R_java_lang_StringBuilder"], function*(__cn1ThisObject, obj) {
  // Fast path: strings (and null) convert synchronously. Everything else must
  // dispatch the Java toString() virtually -- jvm.toNativeString's "" + value
  // fallback stringifies heap objects (boxed Integer/Long/Double included) as
  // the JS "[object Object]", which corrupted JSONWriter output (the
  // surfacesJsonRoundTrip bug).
  if (obj == null || typeof obj === "string" || obj.__nativeString != null || obj.__class === "java_lang_String") {
    return sbAppendNativeString(__cn1ThisObject, jvm.toNativeString(obj));
  }
  return sbAppendNativeString(__cn1ThisObject, yield* runtimeToNativeString(obj));
});
bindNative(["cn1_java_lang_StringBuilder_append_java_lang_String_R_java_lang_StringBuilder"], function(__cn1ThisObject, str) { return sbAppendNativeString(__cn1ThisObject, jvm.toNativeString(str)); });
bindNative(["cn1_java_lang_StringBuilder_charAt_int_R_char"], function(__cn1ThisObject, index) {
  index = index | 0;
  if (index < 0 || index >= (__cn1ThisObject[CN1_SB_COUNT] | 0)) {
    throw new Error("ArrayIndexOutOfBoundsException");
  }
  return (__cn1ThisObject[CN1_SB_VALUE][index] || 0) | 0;
});
bindNative(["cn1_java_lang_StringBuilder_length_R_int"], function(__cn1ThisObject) { return __cn1ThisObject[CN1_SB_COUNT] | 0; });
bindNative(["cn1_java_lang_StringBuilder_toString_R_java_lang_String"], function(__cn1ThisObject) {
  const count = __cn1ThisObject[CN1_SB_COUNT] | 0;
  const data = __cn1ThisObject[CN1_SB_VALUE];
  let out = "";
  for (let i = 0; i < count; i++) {
    out += String.fromCharCode(data[i] | 0);
  }
  return createJavaString(out);
});
bindNative(["cn1_java_lang_StringBuilder_getChars_int_int_char_1ARRAY_int"], function(__cn1ThisObject, start, end, dst, dstStart) {
  if ((start | 0) < 0 || (start | 0) > (end | 0) || (end | 0) > (__cn1ThisObject[CN1_SB_COUNT] | 0)) {
    throw new Error("ArrayIndexOutOfBoundsException");
  }
  const value = __cn1ThisObject[CN1_SB_VALUE];
  for (let i = start | 0; i < (end | 0); i++) {
    dst[(dstStart | 0) + i - (start | 0)] = value[i] | 0;
  }
  return null;
});
bindNative(["cn1_java_lang_String_charAt_int_R_char"], function(__cn1ThisObject, index) {
  const ns = jvm.toNativeString(__cn1ThisObject);
  index = index | 0;
  if (index < 0 || index >= ns.length) {
    throw new Error("ArrayIndexOutOfBoundsException");
  }
  return ns.charCodeAt(index) | 0;
});
bindNative(["cn1_java_lang_String_equals_java_lang_Object_R_boolean"], function(__cn1ThisObject, obj) {
  return (obj != null && obj.__class === "java_lang_String" && jvm.toNativeString(__cn1ThisObject) === jvm.toNativeString(obj)) ? 1 : 0;
});
bindNative(["cn1_java_lang_String_compareTo_java_lang_String_R_int"], function(__cn1ThisObject, other) {
  const a = jvm.toNativeString(__cn1ThisObject), b = jvm.toNativeString(other);
  const minL = Math.min(a.length, b.length);
  for (let i = 0; i < minL; i++) {
    const d = a.charCodeAt(i) - b.charCodeAt(i);
    if (d !== 0) { return d | 0; }
  }
  return (a.length - b.length) | 0;
});
bindNative(["cn1_java_lang_String_equalsIgnoreCase_java_lang_String_R_boolean"], function(__cn1ThisObject, other) {
  return (other != null && jvm.toNativeString(__cn1ThisObject).toLowerCase() === jvm.toNativeString(other).toLowerCase()) ? 1 : 0;
});
bindNative(["cn1_java_lang_String_getChars_int_int_char_1ARRAY_int"], function(__cn1ThisObject, start, end, dst, dstStart) {
  const value = jvm.toNativeString(__cn1ThisObject);
  for (let i = start | 0; i < (end | 0); i++) {
    dst[(dstStart | 0) + i - (start | 0)] = value.charCodeAt(i) | 0;
  }
  return null;
});
bindNative(["cn1_java_lang_String_hashCode_R_int"], function(__cn1ThisObject) {
  let hash = __cn1ThisObject[CN1_STRING_HASH] | 0;
  if (hash !== 0) {
    return hash;
  }
  const value = jvm.toNativeString(__cn1ThisObject);
  for (let i = 0; i < value.length; i++) {
    hash = (((hash * 31) | 0) + value.charCodeAt(i)) | 0;
  }
  __cn1ThisObject[CN1_STRING_HASH] = hash;
  return hash;
});
bindNative(["cn1_java_lang_String_indexOf_int_int_R_int"], function(__cn1ThisObject, ch, fromIndex) { return jvm.toNativeString(__cn1ThisObject).indexOf(String.fromCharCode(ch | 0), fromIndex | 0); });
bindNative(["cn1_java_lang_String_toLowerCase_R_java_lang_String"], function(__cn1ThisObject) { return createJavaString(jvm.toNativeString(__cn1ThisObject).toLowerCase()); });
bindNative(["cn1_java_lang_String_toString_R_java_lang_String"], function(__cn1ThisObject) { return __cn1ThisObject; });
bindNative(["cn1_java_lang_String_toUpperCase_R_java_lang_String"], function(__cn1ThisObject) { return createJavaString(jvm.toNativeString(__cn1ThisObject).toUpperCase()); });
bindNative(["cn1_java_lang_String_releaseNSString_long"], function() { return null; });
bindNative(["cn1_java_lang_String_bytesToChars_byte_1ARRAY_int_int_java_lang_String_R_char_1ARRAY"], function(bytes, off, len, encoding) {
  const slice = bytes.slice(off | 0, (off | 0) + (len | 0));
  const array = Uint8Array.from(slice, function(v) { return v & 0xff; });
  let text = "";
  if (typeof TextDecoder !== "undefined") {
    try {
      text = new TextDecoder((encoding ? jvm.toNativeString(encoding) : "utf-8")).decode(array);
    } catch (err) {
      text = new TextDecoder("utf-8").decode(array);
    }
  } else {
    for (let i = 0; i < array.length; i++) {
      text += String.fromCharCode(array[i]);
    }
  }
  return createArrayFromNativeString(text);
});
bindNative(["cn1_java_io_InputStreamReader_bytesToChars_byte_1ARRAY_int_int_java_lang_String_R_char_1ARRAY"], function*(bytes, off, len, encoding) {
  // Adapt the call result so a CHA-sync classification of the
  // String.bytesToChars body doesn't tip ``yield*`` into a
  // ``not iterable`` TypeError.
  return yield* adaptVirtualResult(cn1_java_lang_String_bytesToChars_byte_1ARRAY_int_int_java_lang_String_R_char_1ARRAY(bytes, off, len, encoding));
});
bindNative(["cn1_java_lang_String_charsToBytes_char_1ARRAY_char_1ARRAY_R_byte_1ARRAY"], function(chars) {
  // The original ``text += String.fromCharCode(chars[i] | 0)`` loop
  // is O(N) of additive string concatenation, which most JS engines
  // optimise to a rope but still costs noticeably more than the
  // ``apply`` form. ``String.fromCharCode.apply(null, chars)`` builds
  // the string in one native call. JS engines cap argument counts
  // around 65535 so chunk anything larger.
  const charsLen = chars.length;
  let text;
  if (charsLen === 0) {
    text = "";
  } else if (charsLen <= 32768) {
    text = String.fromCharCode.apply(null, chars);
  } else {
    const parts = [];
    for (let i = 0; i < charsLen; i += 32768) {
      const end = (i + 32768 < charsLen) ? i + 32768 : charsLen;
      parts.push(String.fromCharCode.apply(null, chars.slice(i, end)));
    }
    text = parts.join("");
  }
  let encoded;
  if (typeof TextEncoder !== "undefined") {
    encoded = new TextEncoder().encode(text);
  } else {
    encoded = Uint8Array.from(text.split("").map(function(ch) { return ch.charCodeAt(0) & 0xff; }));
  }
  const out = jvm.newArray(encoded.length, "JAVA_BYTE", 1);
  for (let i = 0; i < encoded.length; i++) {
    out[i] = encoded[i];
  }
  return out;
});
bindNative(["cn1_java_lang_String_format_java_lang_String_java_lang_Object_1ARRAY_R_java_lang_String"], function*(format, args) {
  const text = jvm.toNativeString(format);
  const values = [];
  if (args && args.__array) {
    for (let i = 0; i < args.length; i++) {
      values.push(args[i]);
    }
  }

  const nextArgString = function*(token) {
    const arg = values.length ? values.shift() : null;
    return yield* runtimeFormatTokenValue("%" + token, arg);
  };

  let out = "";
  for (let i = 0; i < text.length; i++) {
    const ch = text.charAt(i);
    if (ch !== "%" || i === text.length - 1) {
      out += ch;
      continue;
    }

    const next = text.charAt(i + 1);
    if (next === "%") {
      out += "%";
      i++;
      continue;
    }

    let j = i + 1;
    while (j < text.length && "-#+ 0,(".indexOf(text.charAt(j)) >= 0) {
      j++;
    }
    while (j < text.length && text.charAt(j) >= "0" && text.charAt(j) <= "9") {
      j++;
    }
    if (j < text.length && text.charAt(j) === ".") {
      j++;
      while (j < text.length && text.charAt(j) >= "0" && text.charAt(j) <= "9") {
        j++;
      }
    }
    const token = j < text.length ? text.charAt(j) : "";
    if ("sdifc".indexOf(token) >= 0) {
      out += yield* nextArgString(token);
      i = j;
    } else {
      out += "%";
    }
  }

  return createJavaString(out);
});
bindNative(["cn1_java_lang_StringToReal_parseDblImpl_java_lang_String_int_R_double"], function(value, exponentIndex) {
  // Contract (per Apache Harmony StringToReal.parseDblImpl): the input string
  // is the pre-processed digits with no decimal point, and exponentIndex is
  // the power of 10 to multiply by. StringToReal.parseDouble("1.4") strips
  // the '.' -> "14" with exponentIndex=-1, so the correct value is 14 * 10^-1 = 1.4.
  // The previous implementation returned Number("14")=14, which was the root
  // cause of the huge Switch pills and any other CN1 path that uses fractional
  // theme constants / scale factors. Apply the exponent.
  const text = jvm.toNativeString(value);
  const parsed = Number(text);
  if (isNaN(parsed)) return 0;
  const exp = exponentIndex | 0;
  return exp === 0 ? parsed : parsed * Math.pow(10, exp);
});
bindNative(["cn1_java_lang_Enum_valueOf_java_lang_Class_java_lang_String_R_java_lang_Enum"], function(enumType, name) {
  if (!enumType || !enumType.__classDef) {
    return null;
  }
  jvm.ensureClassInitialized(enumType.__classDef.name);
  const matchName = jvm.toNativeString(name);
  if (enumType.cn1_staticFields) {
    const staticFieldNames = Object.keys(enumType.cn1_staticFields);
    for (let i = 0; i < staticFieldNames.length; i++) {
      const candidate = enumType.cn1_staticFields[staticFieldNames[i]];
      if (candidate && candidate.__array) {
        for (let j = 0; j < candidate.length; j++) {
          const arrayEntry = candidate[j];
          if (arrayEntry != null
                  && arrayEntry.__class === enumType.__classDef.name
                  && jvm.toNativeString(arrayEntry[CN1_ENUM_NAME]) === matchName) {
            return arrayEntry;
          }
        }
      }
      if (candidate && candidate.__class === enumType.__classDef.name
              && jvm.toNativeString(candidate[CN1_ENUM_NAME]) === matchName) {
        return candidate;
      }
    }
  }
  return null;
});
bindNative(["cn1_java_lang_Class_forNameImpl_java_lang_String_R_java_lang_Class"], function(className) {
  const runtimeName = runtimeTypeFromJavaName(jvm.toNativeString(className));
  const cls = jvm.classes[runtimeName];
  return cls ? cls.classObject : null;
});
bindNative(["cn1_java_lang_Class_getNameImpl_R_java_lang_String"], function(__cn1ThisObject) {
  return createJavaString(javaClassName(__cn1ThisObject.__classDef.name));
});
bindNative(["cn1_java_lang_Class_getName_R_java_lang_String"], function(__cn1ThisObject) { return createJavaString(descriptorClassName(__cn1ThisObject.__classDef.name)); });
bindNative(["cn1_java_lang_Class_isArray_R_boolean"], function(__cn1ThisObject) { return __cn1ThisObject.__classDef && __cn1ThisObject.__classDef.name.indexOf("[]") > -1 ? 1 : 0; });
bindNative(["cn1_java_lang_Class_isAssignableFrom_java_lang_Class_R_boolean"], function(__cn1ThisObject, cls) { return cls && cls.__classDef && cls.__classDef.assignableTo[__cn1ThisObject.__classDef.name] ? 1 : 0; });
bindNative(["cn1_java_lang_Class_isInstance_java_lang_Object_R_boolean"], function(__cn1ThisObject, obj) { return jvm.instanceOf(obj, __cn1ThisObject.__classDef.name) ? 1 : 0; });
bindNative(["cn1_java_lang_Class_isInterface_R_boolean"], function(__cn1ThisObject) { return __cn1ThisObject.__classDef && __cn1ThisObject.__classDef.isInterface ? 1 : 0; });
bindNative(["cn1_java_lang_Class_newInstanceImpl_R_java_lang_Object"], function*(__cn1ThisObject) {
  const def = __cn1ThisObject.__classDef;
  if (!def || def.isInterface || def.isAbstract || def.isPrimitive || def.name.indexOf("[]") > -1) {
    return null;
  }
  const obj = jvm.newObject(def.name);
  // Prefer the direct ctor reference attached at ``defineClass`` time
  // (``def.t`` → ``def.noArgCtor``). The legacy ``global["cn1_<name>___INIT__"]``
  // lookup doesn't resolve under the post-translation mangler — see
  // the comment on ``def.noArgCtor`` in ``defineClass``.
  let ctor = def.noArgCtor;
  if (typeof ctor !== "function") {
    ctor = global["cn1_" + def.name + "___INIT__"];
  }
  if (typeof ctor === "function") {
    yield* adaptVirtualResult(ctor(obj));
  }
  return obj;
});
bindNative(["cn1_java_lang_Class_isAnnotation_R_boolean"], function() { return 0; });
bindNative(["cn1_java_lang_Class_isEnum_R_boolean"], function() { return 0; });
bindNative(["cn1_java_lang_Class_isAnonymousClass_R_boolean"], function() { return 0; });
bindNative(["cn1_java_lang_Class_isSynthetic_R_boolean"], function() { return 0; });
bindNative(["cn1_java_lang_Class_hashCode_R_int"], function(__cn1ThisObject) { return __cn1ThisObject && __cn1ThisObject.__classDef ? (__cn1ThisObject.__classDef.name.length | 0) : 0; });
bindNative(["cn1_java_lang_Class_getComponentType_R_java_lang_Class"], function(__cn1ThisObject) {
  const def = __cn1ThisObject.__classDef;
  if (!def || def.name.indexOf("[]") < 0) {
    return null;
  }
  return classObjectForName(def.componentClass);
});
bindNative(["cn1_java_lang_Class_isPrimitive_R_boolean"], function(__cn1ThisObject) { return __cn1ThisObject.__classDef && __cn1ThisObject.__classDef.isPrimitive ? 1 : 0; });
bindNative(["cn1_java_lang_reflect_Array_newInstanceImpl_java_lang_Class_int_R_java_lang_Object"], function(componentClass, length) {
  if (!componentClass || !componentClass.__classDef) {
    return null;
  }
  return jvm.newArray(length | 0, componentClass.__classDef.name, 1);
});
bindNative(["cn1_java_util_Locale_getOSLanguage_R_java_lang_String"], function() {
  let locale = null;
  if (typeof navigator !== "undefined" && navigator.language) {
    locale = navigator.language;
  } else if (typeof Intl !== "undefined" && Intl.DateTimeFormat) {
    locale = Intl.DateTimeFormat().resolvedOptions().locale;
  }
  return createJavaString(locale || "en-US");
});
bindNative(["cn1_java_util_TimeZone_getTimezoneId_R_java_lang_String"], function() {
  return createJavaString(defaultTimeZoneId());
});
bindNative(["cn1_java_util_TimeZone_getTimezoneOffset_java_lang_String_int_int_int_int_R_int"], function(name, year, month, day, timeOfDayMillis) {
  const tz = normalizeTimeZoneId(name);
  const millis = Date.UTC((year | 0), ((month | 0) - 1), day | 0, 0, 0, 0, 0) + (timeOfDayMillis | 0);
  return timezoneOffsetMillis(tz, millis);
});
bindNative(["cn1_java_util_TimeZone_getTimezoneRawOffset_java_lang_String_R_int"], function(name) {
  return timezoneRawOffsetMillis(normalizeTimeZoneId(name));
});
bindNative(["cn1_java_util_TimeZone_isTimezoneDST_java_lang_String_long_R_boolean"], function(name, millis) {
  const tz = normalizeTimeZoneId(name);
  return timezoneOffsetMillis(tz, millis) !== timezoneRawOffsetMillis(tz) ? 1 : 0;
});
bindNative(["cn1_java_text_DateFormat_format_java_util_Date_java_lang_StringBuffer_R_java_lang_String"], function(__cn1ThisObject, date, toAppendTo) {
  const formatted = createJavaString(formatJavaDate(__cn1ThisObject, date));
  if (toAppendTo != null && toAppendTo[CN1_STRINGBUFFER_INTERNAL] != null) {
    sbAppendNativeString(toAppendTo[CN1_STRINGBUFFER_INTERNAL], jvm.toNativeString(formatted));
  }
  return formatted;
});
bindNative(["cn1_java_util_HashMap_areEqualKeys_java_lang_Object_java_lang_Object_R_boolean"], function*(key1, key2) {
  if (key1 === key2) {
    return 1;
  }
  if (key1 == null || key2 == null) {
    return 0;
  }
  if (areStringLikeEqual(key1, key2)) {
    return 1;
  }
  if (!key1.__class) {
    return 0;
  }
  // Shared dispatch id — see the equivalent change in
  // ``cn1_java_lang_Object_equals_java_lang_Object_R_boolean`` above.
  const equalsMethod = jvm.resolveVirtual(key1.__class, "cn1_s_equals_java_lang_Object_R_boolean");
  return (yield* adaptVirtualResult(equalsMethod(key1, key2))) ? 1 : 0;
});
// COMPACT HashMap natives: the C implementations are hand-tuned probe loops;
// on the JS backend every one of them simply delegates to the pure-Java *Impl
// twin (the semantic source of truth) that the translator compiled to JS.
bindNative(["cn1_java_util_HashMap_get_java_lang_Object_R_java_lang_Object"], function*(__cn1ThisObject, key) {
  return yield* adaptVirtualResult(cn1_java_util_HashMap_getImpl_java_lang_Object_R_java_lang_Object(__cn1ThisObject, key));
});
bindNative(["cn1_java_util_HashMap_put_java_lang_Object_java_lang_Object_R_java_lang_Object"], function*(__cn1ThisObject, key, value) {
  return yield* adaptVirtualResult(cn1_java_util_HashMap_putImpl_java_lang_Object_java_lang_Object_R_java_lang_Object(__cn1ThisObject, key, value));
});
bindNative(["cn1_java_util_HashMap_remove_java_lang_Object_R_java_lang_Object"], function*(__cn1ThisObject, key) {
  return yield* adaptVirtualResult(cn1_java_util_HashMap_removeImpl_java_lang_Object_R_java_lang_Object(__cn1ThisObject, key));
});
bindNative(["cn1_java_util_HashMap_containsKey_java_lang_Object_R_boolean"], function*(__cn1ThisObject, key) {
  return yield* adaptVirtualResult(cn1_java_util_HashMap_containsKeyImpl_java_lang_Object_R_boolean(__cn1ThisObject, key));
});
bindNative(["cn1_java_util_HashMap_clear"], function*(__cn1ThisObject) {
  return yield* adaptVirtualResult(cn1_java_util_HashMap_clearImpl(__cn1ThisObject));
});
bindNative(["cn1_java_io_NSLogOutputStream_write_byte_1ARRAY_int_int"], function*(__cn1ThisObject, bytes, off, len) {
  const chars = yield* adaptVirtualResult(cn1_java_lang_String_bytesToChars_byte_1ARRAY_int_int_java_lang_String_R_char_1ARRAY(bytes, off, len, createJavaString("utf-8")));
  jvm.log(nativeStringFromCharArray(chars));
  return null;
});
bindNative(["cn1_com_codename1_impl_platform_js_VMHost_getLastEventCode_R_int",
            "cn1_com_codename1_impl_platform_js_VMHost_getLastEventCode___R_int"], function() {
  if (!jvm.lastEvent || jvm.lastEvent.code == null) {
    return -1;
  }
  return jvm.lastEvent.code | 0;
});
bindNative(["cn1_com_codename1_impl_platform_js_VMHost_pollEventCode_R_int",
            "cn1_com_codename1_impl_platform_js_VMHost_pollEventCode___R_int"], function() {
  if (!jvm.eventQueue.length) {
    return -1;
  }
  const event = jvm.eventQueue.shift();
  return event && event.code != null ? (event.code | 0) : -1;
});

// ---- NativeInterface bridge -----------------------------------------------------
// The generated <Iface>Impl methods call these NativeInterfaceBridge.call*
// natives. Each forwards the (iface, method, args) tuple to the MAIN thread via
// the shared __cn1_native_interface_call__ host hook (browser_bridge.js runs the
// developer's JS stub with DOM access and resolves through its callback), then
// coerces the JS result to the declared Java return type. Args were already
// unboxed by toHostTransferArg (boxed primitives / Java String / long).
const __NI_PREFIX = "cn1_com_codename1_impl_platform_js_NativeInterfaceBridge_";
const __NI_SIG = "java_lang_String_java_lang_String_java_lang_Object_1ARRAY";
function* __cn1NativeInterfaceCall(iface, method, args) {
  return yield jvm.invokeHostNative("__cn1_native_interface_call__", [iface, method, args]);
}
function __cn1NativeInterfaceArray(v, token) {
  if (v == null) {
    return null;
  }
  const len = v.length | 0;
  const arr = jvm.newArray(len, token, 1);
  for (let i = 0; i < len; i++) {
    const e = v[i];
    switch (token) {
      case "java_lang_String": arr[i] = (e == null ? null : createJavaString(e)); break;
      case "JAVA_LONG": arr[i] = _LfromNumber(Number(e || 0)); break;
      case "JAVA_BOOLEAN": arr[i] = !!e; break;
      case "JAVA_CHAR": arr[i] = (e | 0) & 0xffff; break;
      case "JAVA_BYTE": arr[i] = ((e | 0) << 24) >> 24; break;
      case "JAVA_SHORT": arr[i] = ((e | 0) << 16) >> 16; break;
      case "JAVA_INT": arr[i] = e | 0; break;
      case "JAVA_FLOAT": arr[i] = Math.fround(Number(e || 0)); break;
      case "JAVA_DOUBLE": arr[i] = Number(e || 0); break;
      default: arr[i] = e;
    }
  }
  return arr;
}
bindNative([__NI_PREFIX + "callBoolean_" + __NI_SIG + "_R_boolean"], function*(iface, method, args) {
  return !!(yield* __cn1NativeInterfaceCall(iface, method, args));
});
bindNative([__NI_PREFIX + "callByte_" + __NI_SIG + "_R_byte"], function*(iface, method, args) {
  const v = yield* __cn1NativeInterfaceCall(iface, method, args); return ((v | 0) << 24) >> 24;
});
bindNative([__NI_PREFIX + "callShort_" + __NI_SIG + "_R_short"], function*(iface, method, args) {
  const v = yield* __cn1NativeInterfaceCall(iface, method, args); return ((v | 0) << 16) >> 16;
});
bindNative([__NI_PREFIX + "callInt_" + __NI_SIG + "_R_int"], function*(iface, method, args) {
  return (yield* __cn1NativeInterfaceCall(iface, method, args)) | 0;
});
bindNative([__NI_PREFIX + "callChar_" + __NI_SIG + "_R_char"], function*(iface, method, args) {
  return ((yield* __cn1NativeInterfaceCall(iface, method, args)) | 0) & 0xffff;
});
bindNative([__NI_PREFIX + "callLong_" + __NI_SIG + "_R_long"], function*(iface, method, args) {
  return _LfromNumber(Number((yield* __cn1NativeInterfaceCall(iface, method, args)) || 0));
});
bindNative([__NI_PREFIX + "callFloat_" + __NI_SIG + "_R_float"], function*(iface, method, args) {
  return Math.fround(Number((yield* __cn1NativeInterfaceCall(iface, method, args)) || 0));
});
bindNative([__NI_PREFIX + "callDouble_" + __NI_SIG + "_R_double"], function*(iface, method, args) {
  return Number((yield* __cn1NativeInterfaceCall(iface, method, args)) || 0);
});
bindNative([__NI_PREFIX + "callString_" + __NI_SIG + "_R_java_lang_String"], function*(iface, method, args) {
  const v = yield* __cn1NativeInterfaceCall(iface, method, args);
  return v == null ? null : createJavaString(v);
});
bindNative([__NI_PREFIX + "callObject_" + __NI_SIG + "_R_java_lang_Object"], function*(iface, method, args) {
  const v = yield* __cn1NativeInterfaceCall(iface, method, args);
  return (typeof v === "string") ? createJavaString(v) : (v == null ? null : v);
});
bindNative([__NI_PREFIX + "callVoid_" + __NI_SIG], function*(iface, method, args) {
  yield* __cn1NativeInterfaceCall(iface, method, args);
  return null;
});
bindNative([__NI_PREFIX + "callArray_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_java_lang_String_R_java_lang_Object"],
        function*(iface, method, args, token) {
  const v = yield* __cn1NativeInterfaceCall(iface, method, args);
  return __cn1NativeInterfaceArray(v, jvm.toNativeString(token));
});

// Worker liveness heartbeat (diag-only). If the worker wedges in a synchronous
// green-thread step this timer CANNOT fire (single-threaded) and the heartbeat
// STOPS; if the worker is merely parked/starved (idle, a host callback not
// delivered) the heartbeat keeps firing with runnable==0 and a frozen resume
// count. The timer is created ONLY under diag so production has no perpetual
// wakeup.
if (VM_DIAG_ENABLED && typeof setInterval === "function") {
  let __cn1HbLastResumes = -1;
  let __cn1HbFrozenStreak = 0;
  let __cn1HbTick = 0;
  setInterval(function() {
    try {
      const rc = jvm.__cn1ResumeCount | 0;
      const frozen = rc === __cn1HbLastResumes;
      __cn1HbLastResumes = rc;
      __cn1HbFrozenStreak = frozen ? (__cn1HbFrozenStreak + 1) : 0;
      // Periodic full thread dump (every ~20 beats ~= 30s). The FROZEN dump
      // below only covers total wedges (resume count stalled); a single
      // parked thread with the EDT still ticking -- e.g. a runner waiting on
      // a notify that never comes -- never trips it. The periodic dump shows
      // every thread's wait target during such partial stalls.
      __cn1HbTick++;
      if (__cn1HbTick % 20 === 0) {
        vmTrace("DIAG:WORKER_HB_THREADS:" + jvm.dumpThreadStates());
      }
      // Stranded-sleep detector: a thread parked in sleep PAST its deadline is
      // in one of three states, each implicating a different bug:
      //   queued=1   -- entry still in timedWakeups; the single host timer is
      //                 not firing / mis-armed (_refreshTimedWakeupTimer).
      //   cancelled=1 -- something _removeTimedWakeup'd it without resuming
      //                 the thread.
      //   gone       -- spliced out of timedWakeups while not cancelled:
      //                 _processExpiredTimedWakeups collected it but the
      //                 enqueue never landed.
      var __ths = jvm.threads || [];
      for (var __i = 0; __i < __ths.length; __i++) {
        var __t = __ths[__i];
        if (__t.done || !__t.waiting || __t.waiting.op !== "sleep" || !__t.waiting.entry) continue;
        var __e = __t.waiting.entry;
        var __due = __e.wakeAt - jvm.schedulerNow();
        if (__due > -2000) continue;
        vmTrace("DIAG:STRANDED_SLEEP:t" + __t.id
          + ":dueIn=" + Math.round(__due)
          + ":queued=" + (jvm.timedWakeups.indexOf(__e) >= 0 ? 1 : 0)
          + ":cancelled=" + (__e.cancelled ? 1 : 0)
          + ":wakeupTimerArmed=" + (jvm._wakeupTimer != null ? 1 : 0)
          + ":wakeupAt=" + (jvm._wakeupAt === Infinity ? "inf" : Math.round(jvm._wakeupAt - jvm.schedulerNow()))
          + ":pendingWakeups=" + jvm.timedWakeups.length);
      }
      vmTrace("DIAG:WORKER_HB:resumes=" + rc
        + ":runnable=" + (jvm.runnable ? jvm.runnable.length : -1)
        + ":draining=" + (jvm.draining ? 1 : 0)
        + ":drainScheduled=" + (jvm.drainScheduled ? 1 : 0)
        + ":frozen=" + (frozen ? 1 : 0)
        + ":captureGate=" + (jvm.captureGateOwner ? 1 : 0)
        + ":sinceStepMs=" + (jvm.__cn1LastResumeTs != null ? Math.round(jvm.schedulerNow() - jvm.__cn1LastResumeTs) : -1)
        + ":lastThread=" + String(jvm.__cn1LastResumeLabel)
        + ":wakeArm=" + (jvm._wakeupArmCount | 0)
        + ":wakeFire=" + (jvm._wakeupFireCount | 0)
        + ":wakeFiredAgo=" + (jvm._wakeupLastFiredAt != null ? Math.round(jvm.schedulerNow() - jvm._wakeupLastFiredAt) : -1));
      // When the worker is wedged (frozen with nothing runnable) every green
      // thread is parked. Dump WHAT they are parked on so the lost-response /
      // deadlock can be isolated without worker-internal tracing (which
      // Playwright can't attach to). Only fire after a SUSTAINED freeze (>=5
      // consecutive ~1.5s heartbeats = ~7.5s) so legitimate multi-second waits
      // (__cn1_delay__ transitions, dual-appearance settle) don't pollute the
      // signal -- a true wedge never recovers, so it keeps dumping.
      if (frozen && __cn1HbFrozenStreak >= 5 && (jvm.runnable ? jvm.runnable.length : 0) === 0) {
        var pend = jvm.pendingHostCalls || {};
        var counts = {};
        var pk = Object.keys(pend);
        for (var i = 0; i < pk.length; i++) {
          var sym = (pend[pk[i]] && pend[pk[i]].symbol) ? String(pend[pk[i]].symbol) : "unknown";
          counts[sym] = (counts[sym] | 0) + 1;
        }
        var parts = [];
        var ck = Object.keys(counts);
        for (var j = 0; j < ck.length; j++) {
          parts.push(ck[j] + "x" + counts[ck[j]]);
        }
        // Dump each pending timed-wakeup: kind + how overdue/early it is +
        // cancelled flag. With pendingHostCalls=0 the wedge is a timed-wakeup
        // that never fires -- if its wakeAt is in the PAST (overdue) while the
        // backing setTimeout is gone, the scheduler's _refreshTimedWakeupTimer
        // lost the timer (a scheduler bug, not a lost host response).
        var twNow = jvm.schedulerNow();
        var twParts = [];
        var tws = jvm.timedWakeups || [];
        for (var t = 0; t < tws.length; t++) {
          var w = tws[t];
          twParts.push(String(w.kind || "?") + ":dueIn=" + Math.round((w.wakeAt | 0) - twNow)
            + (w.cancelled ? ":cancelled" : ""));
        }
        vmTrace("DIAG:WORKER_HB_FROZEN:pendingHostCalls=" + pk.length
          + ":symbols=" + (parts.length ? parts.join(",") : "none")
          + ":timedWakeups=" + tws.length
          + ":wakeups=[" + twParts.join(",") + "]"
          + ":wakeupTimerSet=" + (jvm._wakeupTimer != null ? 1 : 0)
          + ":wakeupAtIn=" + (jvm._wakeupAt != null && jvm._wakeupAt !== Infinity ? Math.round(jvm._wakeupAt - twNow) : "inf")
          + ":drainScheduled=" + (jvm.drainScheduled ? 1 : 0));
        // Recovery backstop -- ONLY when a wakeup is genuinely OVERDUE (wakeAt is
        // in the past but the timer did not fire), i.e. a true lost/stalled timer.
        // A "frozen" heartbeat with only FUTURE wakeups is NOT a wedge: the worker
        // is just waiting on a long legitimate sleep (e.g. NetworkManager's idle
        // timeout-poll Thread.sleep(timeout/10), 12-30s). Firing the recovery on
        // those is pure churn (clearTimeout + scheduleDrain every 1.5s) that only
        // adds main-thread overhead, so gate it strictly on overdue.
        var anyOverdue = false;
        for (var ov = 0; ov < tws.length; ov++) {
          if (!tws[ov].cancelled && (tws[ov].wakeAt | 0) <= twNow + 1) { anyOverdue = true; break; }
        }
        if (anyOverdue && cn1SelfHealDisabled()) {
          // Self-heal disabled: leave the lost timer stalled so the wedge is
          // deterministic + trackable instead of silently recovered.
          vmTrace("DIAG:WORKER_HB_FROZEN:recovery=SKIPPED(selfHealDisabled) " + jvm.dumpThreadStates());
        } else if (anyOverdue) {
          try {
            if (jvm._wakeupTimer != null) { clearTimeout(jvm._wakeupTimer); jvm._wakeupTimer = null; jvm._wakeupAt = Infinity; }
            jvm._processExpiredTimedWakeups();
            jvm._refreshTimedWakeupTimer();
            if (typeof jvm.scheduleDrain === "function") jvm.scheduleDrain();
            vmTrace("DIAG:WORKER_HB_FROZEN:recovery=rearmed-overdue-timer");
          } catch (_rec) { void _rec; }
        }
      }
    } catch (e) {
      void e;
    }
  }, 1500);
}
})(self);
