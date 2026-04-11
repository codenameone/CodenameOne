(function(global) {
  function shouldEnableDiag() {
    if (global.__parparDiagEnabled != null) {
      return !!global.__parparDiagEnabled;
    }
    var loc = (global.window || global).location;
    if (!loc || !loc.search) {
      return false;
    }
    var search = String(loc.search).charAt(0) === '?' ? String(loc.search).substring(1) : String(loc.search);
    if (!search) {
      return false;
    }
    var pairs = search.split('&');
    for (var i = 0; i < pairs.length; i++) {
      var entry = pairs[i];
      if (!entry) {
        continue;
      }
      var eq = entry.indexOf('=');
      var key = decodeURIComponent((eq >= 0 ? entry.substring(0, eq) : entry).replace(/\+/g, ' '));
      if (key !== 'parparDiag') {
        continue;
      }
      var rawValue = decodeURIComponent((eq >= 0 ? entry.substring(eq + 1) : '1').replace(/\+/g, ' '));
      var normalized = String(rawValue).toLowerCase();
      return !(normalized === '0' || normalized === 'false' || normalized === 'off' || normalized === 'no');
    }
    return false;
  }

  var diagEnabled = shouldEnableDiag();

  function diagValue(value) {
    if (value == null) {
      return 'null';
    }
    return String(value).replace(/\s+/g, '_');
  }

  function log(line) {
    if (global.console && typeof global.console.log === 'function') {
      global.console.log('PARPAR:' + line);
    }
  }
  function diag(phase, key, value) {
    if (!diagEnabled) {
      return;
    }
    log('DIAG:' + phase + ':' + key + '=' + diagValue(value));
  }

  function postHostCallback(target, id, value, errorMessage) {
    var message;
    if (errorMessage == null) {
      message = { type: 'host-callback', id: id, value: value };
    } else {
      message = { type: 'host-callback', id: id, error: true, errorMessage: String(errorMessage) };
    }
    if (target && typeof target.postMessage === 'function') {
      target.postMessage(message);
      return;
    }
    if (global.jvm && typeof global.jvm.handleMessage === 'function') {
      global.jvm.handleMessage(message);
    }
  }

  function normalizeHostResult(result, callback) {
    if (result && typeof result.then === 'function') {
      result.then(function(value) {
        callback(value, null);
      }, function(err) {
        callback(null, err);
      });
      return;
    }
    callback(result, null);
  }

  var hostBridge = global.cn1HostBridge = global.cn1HostBridge || {
    handlers: {},
    register: function(symbol, handler) {
      this.handlers[symbol] = handler;
    },
    invoke: function(symbol, args, target, id) {
      var handler = this.handlers[symbol];
      if (!handler) {
        diag('FIRST_FAILURE', 'category', 'host_call_unhandled');
        diag('FIRST_FAILURE', 'symbol', symbol);
        postHostCallback(target, id, null, 'Unhandled host call ' + symbol);
        return;
      }
      try {
        normalizeHostResult(handler.apply(null, args || []), function(value, err) {
          postHostCallback(target, id, value, err);
        });
      } catch (err) {
        postHostCallback(target, id, null, err);
      }
    }
  };

  var hostRefNextId = 1;
  var hostRefById = {};
  var hostRefByObject = (typeof WeakMap === 'function') ? new WeakMap() : null;

  function isHostRefMarker(value) {
    return !!(value && typeof value === 'object'
      && value.__cn1HostRef != null
      && value.__cn1HostRef !== 0);
  }

  function storeHostRef(value) {
    if (value == null || (typeof value !== 'object' && typeof value !== 'function')) {
      return value;
    }
    var inferredClass = inferHostClass(value);
    if (hostRefByObject && hostRefByObject.has(value)) {
      var existing = { __cn1HostRef: hostRefByObject.get(value) };
      if (inferredClass) {
        existing.__cn1HostClass = inferredClass;
      }
      return existing;
    }
    var id = hostRefNextId++;
    hostRefById[id] = value;
    if (hostRefByObject) {
      hostRefByObject.set(value, id);
    }
    var marker = { __cn1HostRef: id };
    if (inferredClass) {
      marker.__cn1HostClass = inferredClass;
    }
    return marker;
  }

  function fallbackHostObjectForClass(hostClass) {
    if (!hostClass) {
      return null;
    }
    if (hostClass.indexOf('com_codename1_impl_html5_JSOImplementations_Window') === 0) {
      return global.window || null;
    }
    if (hostClass.indexOf('com_codename1_impl_html5_JSOImplementations_Document') === 0) {
      if (global.document) {
        return global.document;
      }
      return global.window && global.window.document ? global.window.document : null;
    }
    if (hostClass === 'com_codename1_html5_js_browser_Window') {
      return global.window || null;
    }
    if (hostClass === 'com_codename1_html5_js_dom_HTMLDocument') {
      if (global.document) {
        return global.document;
      }
      return global.window && global.window.document ? global.window.document : null;
    }
    if (hostClass === 'com_codename1_html5_js_dom_HTMLBodyElement') {
      var doc = global.document || (global.window && global.window.document);
      return doc && doc.body ? doc.body : null;
    }
    return null;
  }

  function resolveHostRef(marker) {
    if (!isHostRefMarker(marker)) {
      return marker;
    }
    var id = marker.__cn1HostRef;
    var existing = hostRefById[id];
    if (existing != null) {
      return existing;
    }
    var fallback = fallbackHostObjectForClass(marker.__cn1HostClass || null);
    if (fallback != null) {
      hostRefById[id] = fallback;
      if (hostRefByObject) {
        hostRefByObject.set(fallback, id);
      }
      diag('HOST', 'receiverRehydrated', String(marker.__cn1HostClass || 'unknown') + '#' + String(id));
      return fallback;
    }
    return null;
  }

  function mapHostArgs(args) {
    var out = [];
    var list = args || [];
    for (var i = 0; i < list.length; i++) {
      out.push(resolveHostRef(list[i]));
    }
    return out;
  }

  function hostResult(value) {
    if (value == null || typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
      return value;
    }
    return storeHostRef(value);
  }

  function inferHostClass(value) {
    if (value === global.window) {
      return 'com_codename1_html5_js_browser_Window';
    }
    if (typeof Event === 'function' && value instanceof Event) {
      return 'com_codename1_html5_js_dom_Event';
    }
    if (value && value.nodeType === 9) {
      return 'com_codename1_html5_js_dom_HTMLDocument';
    }
    if (value && value.canvas && typeof value.drawImage === 'function' && typeof value.fillRect === 'function') {
      return 'com_codename1_html5_js_canvas_CanvasRenderingContext2D';
    }
    if (value && value.setProperty && value.removeProperty) {
      return 'com_codename1_html5_js_dom_CSSStyleDeclaration';
    }
    if (value && value.tagName) {
      var tagName = String(value.tagName).toUpperCase();
      if (tagName === 'CANVAS') {
        return 'com_codename1_html5_js_dom_HTMLCanvasElement';
      }
      if (tagName === 'IFRAME') {
        return 'com_codename1_impl_html5_JSOImplementations_HTMLIFrameElement';
      }
      if (tagName === 'BODY') {
        return 'com_codename1_html5_js_dom_HTMLBodyElement';
      }
      return 'com_codename1_html5_js_dom_HTMLElement';
    }
    if (value && value.nodeType === 1) {
      return 'com_codename1_html5_js_dom_Element';
    }
    return null;
  }

  hostBridge.register('__cn1_dom_window_current__', function() {
    if (global.window) {
      return hostResult(global.window);
    }
    return null;
  });

  hostBridge.register('__cn1_jso_bridge__', function(request) {
    var payload = request || {};
    var receiver = resolveHostRef(payload.receiver);
    var receiverClassHint = (payload.receiver && payload.receiver.__cn1HostClass)
      || payload.receiverClass
      || null;
    diag('HOST', 'jsoBridgeKind', payload.kind || 'unknown');
    diag('HOST', 'jsoBridgeMember', payload.member || 'unknown');
    if (payload.receiver && payload.receiver.__cn1HostRef != null) {
      diag('HOST', 'jsoBridgeReceiverRef', payload.receiver.__cn1HostRef);
      diag('HOST', 'jsoBridgeReceiverClass', payload.receiver.__cn1HostClass || 'unknown');
    } else {
      diag('HOST', 'jsoBridgeReceiverRef', 'none');
      diag('HOST', 'jsoBridgeReceiverClass', 'none');
    }
    if (receiver == null) {
      receiver = fallbackHostObjectForClass(receiverClassHint);
      if (receiver != null) {
        diag('HOST', 'receiverFallback', String(receiverClassHint || 'unknown'));
      }
    }
    if (receiver == null) {
      if (payload && payload.kind === 'getter' && payload.member === 'document') {
        receiver = global.window || null;
        if (receiver != null) {
          diag('HOST', 'receiverFallback', 'window.document');
        }
      }
    }
    if (receiver == null) {
      diag('FIRST_FAILURE', 'category', 'host_receiver_missing');
      diag('FIRST_FAILURE', 'hostMember', payload.member || 'unknown');
      diag('FIRST_FAILURE', 'hostKind', payload.kind || 'unknown');
      diag('FIRST_FAILURE', 'hostReceiverClass', receiverClassHint || 'none');
      throw new Error('Missing host receiver for JSO bridge');
    }
    var kind = payload.kind;
    var member = payload.member;
    var args = mapHostArgs(payload.args || []);
    var value;
    if (kind === 'getter') {
      value = receiver[member];
    } else if (kind === 'setter') {
      receiver[member] = args.length ? args[0] : null;
      value = null;
    } else {
      var fn = receiver[member];
      if (typeof fn === 'function') {
        value = fn.apply(receiver, args);
      } else if (!args.length && Object.prototype.hasOwnProperty.call(receiver, member)) {
        value = receiver[member];
      } else {
        throw new Error('Missing JS member ' + member + ' for host receiver');
      }
    }
    return hostResult(value);
  });

  hostBridge.register('__cn1_create_custom_event__', function(request) {
    var payload = request || {};
    var type = payload.type == null ? '' : String(payload.type);
    var detail = payload.detail == null ? null : payload.detail;
    var code = payload.code == null ? 0 : (payload.code | 0);
    var targetWindow = global.window || global.self || global;
    var event;
    if (typeof targetWindow.CustomEvent === 'function') {
      event = new targetWindow.CustomEvent(type, {
        detail: detail,
        bubbles: false,
        cancelable: false
      });
    } else if (typeof targetWindow.Event === 'function') {
      event = new targetWindow.Event(type);
      event.detail = detail;
    } else {
      throw new Error('CustomEvent is not available in host environment');
    }
    if (event && event.code == null) {
      try {
        Object.defineProperty(event, 'code', {
          configurable: true,
          enumerable: false,
          writable: true,
          value: code
        });
      } catch (err) {
        event.code = code;
      }
    }
    return hostResult(event);
  });

  hostBridge.register('__cn1_capture_canvas_png__', function() {
    function afterPaint() {
      return new Promise(function(resolve) {
        var win = global.window || global;
        var raf = win && typeof win.requestAnimationFrame === 'function' ? win.requestAnimationFrame.bind(win) : null;
        if (!raf) {
          setTimeout(resolve, 16);
          return;
        }
        raf(function() {
          raf(function() {
            resolve();
          });
        });
      });
    }
    function captureNow() {
      var doc = global.document || (global.window && global.window.document) || null;
      if (!doc || typeof doc.querySelectorAll !== 'function') {
        return '';
      }
      var canvases = doc.querySelectorAll('canvas');
      if (!canvases || !canvases.length) {
        return '';
      }
      function canvasContentScore(canvas) {
        if (!canvas || typeof canvas.getContext !== 'function') {
          return -1;
        }
        var w = canvas.width | 0;
        var h = canvas.height | 0;
        if (w <= 0 || h <= 0) {
          return -1;
        }
        var ctx = null;
        try {
          ctx = canvas.getContext('2d');
        } catch (_err) {
          ctx = null;
        }
        if (!ctx || typeof ctx.getImageData !== 'function') {
          return -1;
        }
        var sampleW = Math.min(48, w);
        var sampleH = Math.min(48, h);
        var startX = ((w - sampleW) / 2) | 0;
        var startY = ((h - sampleH) / 2) | 0;
        var img;
        try {
          img = ctx.getImageData(startX, startY, sampleW, sampleH);
        } catch (_err) {
          return -1;
        }
        if (!img || !img.data || !img.data.length) {
          return -1;
        }
        var data = img.data;
        var opaqueCount = 0;
        var nonWhiteCount = 0;
        for (var i = 0; i < data.length; i += 4) {
          var r = data[i] | 0;
          var g = data[i + 1] | 0;
          var b = data[i + 2] | 0;
          var a = data[i + 3] | 0;
          if (a > 12) {
            opaqueCount++;
            if (!(r >= 248 && g >= 248 && b >= 248)) {
              nonWhiteCount++;
            }
          }
        }
        return (nonWhiteCount * 4) + opaqueCount;
      }

      var best = null;
      var bestArea = -1;
      var bestScore = -1;
      var bestIndex = -1;
      for (var i = 0; i < canvases.length; i++) {
        var c = canvases[i];
        if (!c || typeof c.toDataURL !== 'function') {
          continue;
        }
        var w = (c.width | 0);
        var h = (c.height | 0);
        var area = w * h;
        var score = canvasContentScore(c);
        if (score > bestScore || (score === bestScore && area > bestArea)) {
          bestScore = score;
          bestArea = area;
          best = c;
          bestIndex = i;
        }
      }
      if (!best) {
        return '';
      }
      try {
        var out = String(best.toDataURL('image/png') || '');
        diag('SCREENSHOT_START', 'canvasCount', canvases.length);
        diag('SCREENSHOT_START', 'canvasPick', bestIndex);
        diag('SCREENSHOT_START', 'canvasArea', bestArea);
        diag('SCREENSHOT_START', 'canvasScore', bestScore);
        diag('SCREENSHOT_START', 'pngLen', out.length);
        return out;
      } catch (_err) {
        return '';
      }
    }
    return afterPaint().then(captureNow);
  });

  global.__parparMessages = [];
  global.cn1Initialized = false;
  global.cn1Started = false;
  global.__parparWorker = null;

  function handleVmMessage(data, target) {
    global.__parparMessages.push(data);
    if (!data) {
      return;
    }
    if (data.type === 'host-call') {
      hostBridge.invoke(data.symbol, data.args || [], target || global.__parparWorker, data.id);
      return;
    }
    if (data.type === 'result') {
      global.__parparResult = data;
      global.cn1Started = true;
      return;
    }
    if (data.type === 'error') {
      global.__parparError = data;
      var failure = data.virtualFailure || null;
      if (failure) {
        diag('FIRST_FAILURE', 'category', failure.category || 'runtime_error');
        diag('FIRST_FAILURE', 'methodId', failure.methodId || 'none');
        diag('FIRST_FAILURE', 'receiverClass', failure.receiverClass || 'none');
      } else {
        diag('FIRST_FAILURE', 'category', 'runtime_error');
        diag('FIRST_FAILURE', 'message', data.message || 'unknown');
      }
      return;
    }
    if (data.type === 'log' && data.message) {
      if (global.console && typeof global.console.log === 'function') {
        global.console.log(String(data.message));
      }
      if (String(data.message).indexOf('CN1SS:INFO:suite starting test=') >= 0) {
        diag('SCREENSHOT_START', 'source', 'vm_log');
      }
    }
  }

  function installWorkerMode() {
    log('worker-mode');
    diag('BOOT', 'bridgeMode', 'worker');
    var worker = new Worker('worker.js');
    global.__parparWorker = worker;
    worker.onmessage = function(event) {
      handleVmMessage(event.data, worker);
    };
    worker.onerror = function(error) {
      global.__parparError = {
        type: 'error',
        message: error && error.message ? error.message : String(error)
      };
    };
    return worker;
  }

  var appStarter = null;

  global.startParparVmApp = function() {
    log('startParparVmApp');
    diag('INIT', 'startParparVmApp', 'entered');
    global.cn1Initialized = true;
    if (appStarter) {
      log('appStarter-present');
      appStarter();
    }
  };

  if (typeof Worker !== 'function') {
    var missingWorkerMessage = 'ParparVM requires Worker support; non-worker mode is not supported';
    log('worker-mode-required');
    diag('BOOT', 'bridgeMode', 'worker-only');
    diag('FIRST_FAILURE', 'category', 'worker_missing');
    diag('FIRST_FAILURE', 'message', missingWorkerMessage);
    global.__parparError = { type: 'error', message: missingWorkerMessage };
    return;
  }
  var worker = installWorkerMode();
  appStarter = function() {
    worker.postMessage({ type: 'start' });
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', global.startParparVmApp);
  } else {
    global.startParparVmApp();
  }
  diag('BOOT', 'bridge', 'loaded');
})(self);
