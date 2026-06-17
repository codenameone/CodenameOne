// Minimal localforage shim for the ParparVM JavaScript port.
//
// The Java-side ``com.codename1.teavm.ext.localforage.LocalForage`` class
// was originally written against TeaVM and assumes ``window.localforage``
// is loaded plus ``window.createConfigOptions`` exists (the latter is the
// inlined body of a TeaVM ``@JSBody`` annotation that the ParparVM JS
// pipeline doesn't process). Without these, the LocalForage constructor
// throws ``Missing JS member createConfigOptions for host receiver``
// during boot the first time anything calls ``Storage.getInstance()`` /
// ``FileSystemStorage.getInstance()``.
//
// This shim provides a localStorage-backed implementation that exposes
// the same async-callback API the LocalForage Java wrapper expects. The
// shim is loaded BEFORE ``browser_bridge.js`` so the JSO bridge resolves
// the missing members on the host window without going through the
// ``Missing JS member`` error path.
(function() {
  if (typeof window === "undefined") {
    return;
  }
  // ``ConfigOptions`` was a TeaVM @JSBody factory that returned a fresh
  // empty object — preserve that contract.
  if (typeof window.createConfigOptions !== "function") {
    window.createConfigOptions = function() { return {}; };
  }
  // This synchronous-callback shim MUST own ``window.localforage`` on the
  // ParparVM port. The CN1 worker can't pump the async microtask/Promise loop
  // a real localForage relies on, so its callbacks must fire inline (see the
  // setItem note below). The catch: ``fontmetrics.js`` bundles a real
  // localForage 1.7.3 and globally exposes it, and it loads BEFORE us -- left
  // in place, CN1 Storage hits its Promise-based callback path and the
  // worker-bridged callback blows up with "b is not a function". So install
  // unconditionally, overriding any real localForage on the window. The only
  // thing we skip is re-installing over OURSELVES (idempotent). fontmetrics
  // keeps its own bundled instance for internal font-metric caching; it
  // references that through its module closure, not ``window.localforage``.
  if (window.localforage && window.localforage.__cn1ShimInstalled) {
    return;
  }
  var STORE_PREFIX = "cn1lf:";
  function namespacedKey(key) { return STORE_PREFIX + String(key); }
  // The Java side blocks on ``done.wait()`` after queueing the setItem
  // request. In TeaVM-land the localforage library returns a Promise
  // and the callback fires asynchronously via setTimeout(0); the
  // ParparVM JS port doesn't have an event-loop pump between the
  // worker-side wait and the host bridge's response, so deferring the
  // callback through ``Promise.resolve().then(...)`` causes Thread A
  // to enter ``done.wait()`` BEFORE the callback's
  // ``done.notifyAll()`` fires (the microtask runs after the bridge
  // has already returned). Drive the callback synchronously: by the
  // time setItem returns, the worker callback proxy has already
  // posted the ``worker-callback`` message and the worker will pick
  // it up the moment Thread A yields on ``done.wait``.
  function callBack(callback, error, value) {
    if (typeof callback === "function") {
      try { callback(error || null, value); }
      catch (_e) { /* user callbacks own their errors */ }
    }
  }
  function setItemImpl(key, value) {
    var serialised;
    var valType = (value == null) ? "null" : (typeof value);
    var valCtor = (value && value.constructor && value.constructor.name) ? value.constructor.name : valType;
    if (value == null) {
      serialised = null;
    } else if (typeof value === "string") {
      serialised = "s:" + value;
    } else {
      try { serialised = "j:" + JSON.stringify(value); }
      catch (_e) { serialised = "j:" + JSON.stringify(String(value)); }
    }
    var nk = namespacedKey(key);
    try {
      if (serialised == null) {
        window.localStorage.removeItem(nk);
      } else {
        window.localStorage.setItem(nk, serialised);
      }
    } catch (e) {
      try {
        console.log("LF-SHIM:set-err key=" + nk + " valType=" + valCtor
                + " serLen=" + (serialised == null ? "null" : serialised.length)
                + " err=" + (e && e.message ? e.message : String(e)));
      } catch (_l) {}
      throw e;
    }
    try {
      console.log("LF-SHIM:set-ok key=" + nk + " valType=" + valCtor
              + " serLen=" + (serialised == null ? "null" : serialised.length)
              + " serPrefix=" + (serialised == null ? "null" : serialised.substring(0, Math.min(60, serialised.length))));
    } catch (_l) {}
    return value;
  }
  function getItemImpl(key) {
    var nk = namespacedKey(key);
    var raw = window.localStorage.getItem(nk);
    try {
      console.log("LF-SHIM:get key=" + nk + " raw=" + (raw == null ? "null" : ("len=" + raw.length + " prefix=" + raw.substring(0, Math.min(40, raw.length)))));
    } catch (_l) {}
    if (raw == null) {
      return null;
    }
    if (raw.indexOf("s:") === 0) {
      return raw.substring(2);
    }
    if (raw.indexOf("j:") === 0) {
      try { return JSON.parse(raw.substring(2)); }
      catch (_e) { return null; }
    }
    return raw;
  }
  function eachKey(callback) {
    var prefix = STORE_PREFIX;
    for (var i = 0; i < window.localStorage.length; i++) {
      var k = window.localStorage.key(i);
      if (k && k.indexOf(prefix) === 0) {
        if (callback(k.substring(prefix.length)) === false) {
          return;
        }
      }
    }
  }
  // Each method synchronously performs the localStorage operation, fires
  // the callback inline, and returns the result. The previous shape
  // ``return (function () { ... });`` returned the function REFERENCE
  // without invoking it -- the storage operation never ran, the callback
  // never fired, and the Java-side polling helper saw the value
  // ``getItem`` returned (null) instead of the value the callback would
  // have delivered. (The Java-side ``setItem-ok`` log still printed
  // because the helper checks ``error[0]`` rather than whether the
  // callback fired. The empty-result case looked indistinguishable from
  // a non-existent key.)
  window.localforage = {
    // Marker so a second load of this shim (or a re-check) doesn't reinstall
    // over itself, while still letting us override a real localForage.
    __cn1ShimInstalled: true,
    INDEXEDDB: "indexeddb",
    WEBSQL: "websql",
    LOCALSTORAGE: "localstorage",
    config: function(_opts) { return true; },
    setItem: function(key, value, callback) {
      var stored = setItemImpl(key, value);
      callBack(callback, null, stored);
      return stored;
    },
    getItem: function(key, callback) {
      var value = getItemImpl(key);
      callBack(callback, null, value);
      return value;
    },
    removeItem: function(key, callback) {
      window.localStorage.removeItem(namespacedKey(key));
      callBack(callback, null);
    },
    clear: function(callback) {
      var doomed = [];
      eachKey(function(k) { doomed.push(k); });
      for (var i = 0; i < doomed.length; i++) {
        window.localStorage.removeItem(namespacedKey(doomed[i]));
      }
      callBack(callback, null);
    },
    length: function(callback) {
      var n = 0;
      eachKey(function() { n++; });
      callBack(callback, null, n);
      return n;
    },
    keys: function(callback) {
      var out = [];
      eachKey(function(k) { out.push(k); });
      callBack(callback, null, out);
      return out;
    },
    iterate: function(iteratorCallback, successCallback) {
      var stopped = false;
      var idx = 1;
      eachKey(function(k) {
        if (stopped) { return false; }
        var value = getItemImpl(k);
        var result;
        try { result = iteratorCallback(value, k, idx++); }
        catch (_e) { result = undefined; }
        if (result !== undefined) {
          stopped = true;
          callBack(successCallback, null, result);
          return false;
        }
      });
      if (!stopped) {
        callBack(successCallback, null);
      }
    },
    // Synchronous, value-returning variants. The ParparVM JS port runs the app
    // in a Web Worker; the legacy callback methods above hand their result back
    // to the worker as a ``worker-callback`` message, which the Java side waited
    // for with a ``while(!done){Thread.sleep(20);}`` poll. That tight timer loop
    // STARVES the worker's ``self.onmessage`` (it never gets a turn to deliver
    // the callback), so every storage op hit the 10s poll timeout and the EDT
    // was permanently blocked -> the whole app froze to input. These *Sync
    // methods are invoked from Java as ordinary BLOCKING JSO host calls that
    // RETURN the value directly (the worker parks on HOST_CALL and resumes on
    // HOST_CALLBACK -- a path that is NOT starved), so no poll/Thread.sleep and
    // no message starvation. localStorage is itself synchronous, so there is
    // nothing async to wait for here.
    getItemSync: function(key) { return getItemImpl(key); },
    setItemSync: function(key, value) { return setItemImpl(key, value); },
    removeItemSync: function(key) { window.localStorage.removeItem(namespacedKey(key)); return true; },
    clearSync: function() {
      var doomed = [];
      eachKey(function(k) { doomed.push(k); });
      for (var i = 0; i < doomed.length; i++) {
        window.localStorage.removeItem(namespacedKey(doomed[i]));
      }
      return true;
    },
    lengthSync: function() { var n = 0; eachKey(function() { n++; }); return n; },
    keysSync: function() { var out = []; eachKey(function(k) { out.push(k); }); return out; }
  };
})();
