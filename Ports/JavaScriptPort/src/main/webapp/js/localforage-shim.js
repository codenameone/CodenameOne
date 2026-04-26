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
  // If a real ``localforage`` library is loaded ahead of us, leave it
  // alone. Otherwise install a localStorage-backed shim.
  if (window.localforage && typeof window.localforage.setItem === "function") {
    return;
  }
  var STORE_PREFIX = "cn1lf:";
  function namespacedKey(key) { return STORE_PREFIX + String(key); }
  function async(fn) {
    return Promise.resolve().then(fn);
  }
  function callBack(callback, error, value) {
    if (typeof callback === "function") {
      try { callback(error || null, value); }
      catch (_e) { /* user callbacks own their errors */ }
    }
  }
  function setItemImpl(key, value) {
    var serialised;
    if (value == null) {
      serialised = null;
    } else if (typeof value === "string") {
      serialised = "s:" + value;
    } else {
      try { serialised = "j:" + JSON.stringify(value); }
      catch (_e) { serialised = "j:" + JSON.stringify(String(value)); }
    }
    if (serialised == null) {
      window.localStorage.removeItem(namespacedKey(key));
    } else {
      window.localStorage.setItem(namespacedKey(key), serialised);
    }
    return value;
  }
  function getItemImpl(key) {
    var raw = window.localStorage.getItem(namespacedKey(key));
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
  window.localforage = {
    INDEXEDDB: "indexeddb",
    WEBSQL: "websql",
    LOCALSTORAGE: "localstorage",
    config: function(_opts) { return true; },
    setItem: function(key, value, callback) {
      return async(function() {
        var stored = setItemImpl(key, value);
        callBack(callback, null, stored);
        return stored;
      });
    },
    getItem: function(key, callback) {
      return async(function() {
        var value = getItemImpl(key);
        callBack(callback, null, value);
        return value;
      });
    },
    removeItem: function(key, callback) {
      return async(function() {
        window.localStorage.removeItem(namespacedKey(key));
        callBack(callback, null);
      });
    },
    clear: function(callback) {
      return async(function() {
        var doomed = [];
        eachKey(function(k) { doomed.push(k); });
        for (var i = 0; i < doomed.length; i++) {
          window.localStorage.removeItem(namespacedKey(doomed[i]));
        }
        callBack(callback, null);
      });
    },
    length: function(callback) {
      return async(function() {
        var n = 0;
        eachKey(function() { n++; });
        callBack(callback, null, n);
        return n;
      });
    },
    keys: function(callback) {
      return async(function() {
        var out = [];
        eachKey(function(k) { out.push(k); });
        callBack(callback, null, out);
        return out;
      });
    },
    iterate: function(iteratorCallback, successCallback) {
      return async(function() {
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
      });
    }
  };
})();
