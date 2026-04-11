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
  var drawMethodNames = {
    clearRect: true,
    fillRect: true,
    strokeRect: true,
    fillText: true,
    strokeText: true,
    drawImage: true,
    putImageData: true,
    fill: true,
    stroke: true
  };

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

  function isCanvasLike(value) {
    return !!(value
      && typeof value.toDataURL === 'function'
      && typeof value.width === 'number'
      && typeof value.height === 'number');
  }

  function noteDrawTarget(receiver, kind, member) {
    if (!receiver || (kind !== 'method' && kind !== 'setter')) {
      return;
    }
    var canvas = null;
    if (isCanvasLike(receiver)) {
      canvas = receiver;
    } else if (receiver.canvas && isCanvasLike(receiver.canvas)) {
      canvas = receiver.canvas;
    }
    if (!canvas) {
      return;
    }
    if (kind === 'method' && !drawMethodNames[member]) {
      return;
    }
    global.__cn1LastDrawCanvas = canvas;
    global.__cn1LastDrawMember = String(member || 'unknown');
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
    noteDrawTarget(receiver, kind, member);
    if (isCanvasLike(receiver) && kind === 'method' && member === 'getContext') {
      global.__cn1LastDrawCanvas = receiver;
      global.__cn1LastDrawMember = 'getContext';
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

  function afterPaint(frames) {
    return new Promise(function(resolve) {
      var win = global.window || global;
      var raf = win && typeof win.requestAnimationFrame === 'function' ? win.requestAnimationFrame.bind(win) : null;
      var remaining = Math.max(1, frames | 0);
      if (!raf) {
        setTimeout(resolve, 16 * remaining);
        return;
      }
      function step() {
        raf(function() {
          remaining--;
          if (remaining <= 0) {
            resolve();
            return;
          }
          step();
        });
      }
      step();
    });
  }

  function shortSignatureFromImageData(img) {
    if (!img || !img.data || !img.data.length) {
      return 'none';
    }
    var data = img.data;
    var hash = 2166136261 >>> 0;
    for (var i = 0; i < data.length; i += 17) {
      hash ^= data[i] | 0;
      hash = Math.imul(hash, 16777619);
    }
    return String((hash >>> 0).toString(16));
  }

  function canvasContentScore(canvas) {
    if (!canvas || typeof canvas.getContext !== 'function') {
      return null;
    }
    var w = canvas.width | 0;
    var h = canvas.height | 0;
    if (w <= 0 || h <= 0) {
      return null;
    }
    var ctx = null;
    try {
      ctx = canvas.getContext('2d');
    } catch (_err) {
      ctx = null;
    }
    if (!ctx || typeof ctx.getImageData !== 'function') {
      return null;
    }
    var sampleW = Math.min(48, w);
    var sampleH = Math.min(48, h);
    var startX = ((w - sampleW) / 2) | 0;
    var startY = ((h - sampleH) / 2) | 0;
    var img;
    try {
      img = ctx.getImageData(startX, startY, sampleW, sampleH);
    } catch (_err) {
      return null;
    }
    if (!img || !img.data || !img.data.length) {
      return null;
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
    return {
      score: (nonWhiteCount * 4) + opaqueCount,
      signature: shortSignatureFromImageData(img)
    };
  }

  function pickBestCanvasSnapshot(includeDataUrl) {
    function pushCanvas(list, seen, canvas, source) {
      if (!canvas || !isCanvasLike(canvas)) {
        return;
      }
      if (seen.indexOf(canvas) >= 0) {
        return;
      }
      seen.push(canvas);
      list.push({ canvas: canvas, source: source });
    }
    var candidates = [];
    var seenCanvases = [];
    var doc = global.document || (global.window && global.window.document) || null;
    if (doc && typeof doc.querySelectorAll === 'function') {
      var domCanvases = doc.querySelectorAll('canvas');
      if (domCanvases && domCanvases.length) {
        for (var di = 0; di < domCanvases.length; di++) {
          pushCanvas(candidates, seenCanvases, domCanvases[di], 'dom');
        }
      }
    }
    pushCanvas(candidates, seenCanvases, global.__cn1LastDrawCanvas || null, 'lastDraw');
    if (hostRefById) {
      var refKeys = Object.keys(hostRefById);
      for (var rk = 0; rk < refKeys.length; rk++) {
        var refVal = hostRefById[refKeys[rk]];
        if (isCanvasLike(refVal)) {
          pushCanvas(candidates, seenCanvases, refVal, 'hostRef');
          continue;
        }
        if (refVal && refVal.canvas && isCanvasLike(refVal.canvas)) {
          pushCanvas(candidates, seenCanvases, refVal.canvas, 'hostRefCanvas');
        }
      }
    }
    if (!candidates.length) {
      return null;
    }
    var best = null;
    var bestArea = -1;
    var bestScore = -1;
    var bestIndex = -1;
    var bestSource = 'none';
    var bestSignature = 'none';
    for (var i = 0; i < candidates.length; i++) {
      var c = candidates[i].canvas;
      if (!c || (includeDataUrl && typeof c.toDataURL !== 'function')) {
        continue;
      }
      var w = (c.width | 0);
      var h = (c.height | 0);
      var area = w * h;
      var scoreMeta = canvasContentScore(c);
      var score = scoreMeta && scoreMeta.score != null ? (scoreMeta.score | 0) : -1;
      var signature = scoreMeta && scoreMeta.signature ? String(scoreMeta.signature) : 'none';
      if (score > bestScore || (score === bestScore && area > bestArea)) {
        bestScore = score;
        bestArea = area;
        best = c;
        bestIndex = i;
        bestSource = candidates[i].source || 'unknown';
        bestSignature = signature;
      }
    }
    if (!best) {
      return null;
    }
    var out = {
      canvasCount: candidates.length,
      canvasPick: bestIndex,
      canvasArea: bestArea,
      canvasScore: bestScore,
      canvasSource: bestSource,
      canvasSignature: bestSignature
    };
    if (!includeDataUrl) {
      return out;
    }
    try {
      out.dataUrl = String(best.toDataURL('image/png') || '');
      return out;
    } catch (_err) {
      return null;
    }
  }

  hostBridge.register('__cn1_wait_for_ui_settle__', function(request) {
    var payload = request || {};
    var reason = payload.reason == null ? 'unknown' : String(payload.reason);
    var maxFrames = Math.max(1, Math.min(48, (payload.maxFrames | 0) || 14));
    var stableFrames = Math.max(1, Math.min(6, (payload.stableFrames | 0) || 2));
    var previousSignature = String(global.__cn1LastScreenshotSignature || '');
    var changed = false;
    var stableCount = 0;
    var lastSignature = '';
    var best = null;
    function chooseBetter(a, b) {
      if (!a) {
        return b;
      }
      if (!b) {
        return a;
      }
      if ((b.canvasScore | 0) !== (a.canvasScore | 0)) {
        return (b.canvasScore | 0) > (a.canvasScore | 0) ? b : a;
      }
      if ((b.canvasArea | 0) !== (a.canvasArea | 0)) {
        return (b.canvasArea | 0) > (a.canvasArea | 0) ? b : a;
      }
      return b;
    }
    function runFrame(index) {
      return afterPaint(1).then(function() {
        var sample = pickBestCanvasSnapshot(false);
        best = chooseBetter(best, sample);
        if (sample && sample.canvasSignature && sample.canvasSignature !== 'none') {
          var sig = String(sample.canvasSignature);
          if (sig !== previousSignature) {
            changed = true;
          }
          if (sig === lastSignature) {
            stableCount++;
          } else {
            stableCount = 1;
            lastSignature = sig;
          }
          if (changed && stableCount >= stableFrames) {
            return sample;
          }
        }
        if (index + 1 >= maxFrames) {
          return best;
        }
        return runFrame(index + 1);
      });
    }
    return runFrame(0).then(function(result) {
      var meta = result || {
        canvasCount: 0,
        canvasPick: -1,
        canvasArea: -1,
        canvasScore: -1,
        canvasSignature: 'none'
      };
      global.__cn1LastUiSettleSignature = meta.canvasSignature || '';
      diag('SCREENSHOT_START', 'settleReason', reason);
      diag('SCREENSHOT_START', 'settleFrames', maxFrames);
      diag('SCREENSHOT_START', 'settleStableFrames', stableFrames);
      diag('SCREENSHOT_START', 'settleChanged', changed ? 1 : 0);
      diag('SCREENSHOT_START', 'settleSig', meta.canvasSignature || 'none');
      diag('SCREENSHOT_START', 'settleScore', meta.canvasScore | 0);
      return {
        changedFromPrevious: changed ? 1 : 0,
        canvasSignature: meta.canvasSignature || 'none',
        canvasScore: meta.canvasScore | 0,
        canvasArea: meta.canvasArea | 0,
        canvasCount: meta.canvasCount | 0,
        canvasPick: meta.canvasPick | 0
      };
    });
  });

  hostBridge.register('__cn1_capture_canvas_png__', function() {
    function captureNow() {
      return pickBestCanvasSnapshot(true);
    }
    var previousSignature = String(global.__cn1LastScreenshotSignature || '');
    var attempts = 8;
    var best = null;
    function chooseBetter(a, b) {
      if (!a) {
        return b;
      }
      if (!b) {
        return a;
      }
      if (!!b.changedFromPrevious !== !!a.changedFromPrevious) {
        return b.changedFromPrevious ? b : a;
      }
      if ((b.canvasScore | 0) !== (a.canvasScore | 0)) {
        return (b.canvasScore | 0) > (a.canvasScore | 0) ? b : a;
      }
      if ((b.canvasArea | 0) !== (a.canvasArea | 0)) {
        return (b.canvasArea | 0) > (a.canvasArea | 0) ? b : a;
      }
      return (String(b.dataUrl || '').length > String(a.dataUrl || '').length) ? b : a;
    }
    function runAttempt(index) {
      return afterPaint(index === 0 ? 2 : 1).then(function() {
        var sample = captureNow();
        if (sample) {
          sample.attempt = index;
          sample.changedFromPrevious = !!(sample.canvasSignature && sample.canvasSignature !== previousSignature);
          best = chooseBetter(best, sample);
          if (sample.changedFromPrevious && (sample.canvasScore | 0) > 0) {
            return sample;
          }
        }
        if (index + 1 >= attempts) {
          return best;
        }
        return runAttempt(index + 1);
      });
    }
    return runAttempt(0).then(function(result) {
      if (!result || !result.dataUrl) {
        return '';
      }
      global.__cn1LastScreenshotSignature = result.canvasSignature || '';
      diag('SCREENSHOT_START', 'canvasCount', result.canvasCount);
      diag('SCREENSHOT_START', 'canvasPick', result.canvasPick);
      diag('SCREENSHOT_START', 'canvasArea', result.canvasArea);
      diag('SCREENSHOT_START', 'canvasScore', result.canvasScore);
      diag('SCREENSHOT_START', 'canvasSource', result.canvasSource || 'unknown');
      diag('SCREENSHOT_START', 'canvasSig', result.canvasSignature || 'none');
      diag('SCREENSHOT_START', 'attempt', result.attempt | 0);
      diag('SCREENSHOT_START', 'changed', result.changedFromPrevious ? 1 : 0);
      diag('SCREENSHOT_START', 'pngLen', String(result.dataUrl || '').length);
      return String(result.dataUrl || '');
    });
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
