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
  // Per-canvas-op bridge diagnostics are gated separately from the generic diag flag
  // because they can grow to 100× the size of the rest of the log (one entry per canvas
  // call). Enable with ?parparBridgeDiag=1 or global.__parparBridgeDiagEnabled=true.
  var bridgeDiagEnabled = (function() {
    if (global.__parparBridgeDiagEnabled != null) {
      return !!global.__parparBridgeDiagEnabled;
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
      if (key !== 'parparBridgeDiag') {
        continue;
      }
      var rawValue = decodeURIComponent((eq >= 0 ? entry.substring(eq + 1) : '1').replace(/\+/g, ' '));
      var normalized = String(rawValue).toLowerCase();
      return !(normalized === '0' || normalized === 'false' || normalized === 'off' || normalized === 'no');
    }
    return false;
  })();

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
    // Suppress per-op HOST bridge diagnostics unless explicitly enabled via
    // parparBridgeDiag=1. These outweigh everything else by a huge margin.
    if (phase === 'HOST' && typeof key === 'string' && key.indexOf('jsoBridge') === 0 && !bridgeDiagEnabled) {
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
  var canvasMetaNextId = 1;
  var canvasMetaByObject = (typeof WeakMap === 'function') ? new WeakMap() : null;
  var canvasMetaById = {};
  var canvasOpSeq = 1;
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
  var canvasStateMemberNames = {
    fillStyle: true,
    strokeStyle: true,
    globalAlpha: true,
    lineWidth: true,
    globalCompositeOperation: true
  };

  function describeCanvasStateValue(value) {
    if (value == null) {
      return 'null';
    }
    if (typeof value === 'string') {
      return value.length > 40 ? (value.substring(0, 40) + '...') : value;
    }
    if (typeof value === 'number' || typeof value === 'boolean') {
      return String(value);
    }
    if (value && typeof value === 'object') {
      if (typeof value.width === 'number' && typeof value.height === 'number') {
        return '[canvas ' + String(value.width | 0) + 'x' + String(value.height | 0) + ']';
      }
      if (value.constructor && value.constructor.name) {
        return '[' + String(value.constructor.name) + ']';
      }
    }
    return String(value);
  }

  function getCanvasMeta(canvas) {
    if (!isCanvasLike(canvas)) {
      return null;
    }
    if (canvasMetaByObject && canvasMetaByObject.has(canvas)) {
      return canvasMetaByObject.get(canvas);
    }
    var meta = {
      id: canvasMetaNextId++,
      canvas: canvas,
      opCount: 0,
      setterCount: 0,
      methodCount: 0,
      paintCount: 0,
      lastSeq: 0,
      lastPaintSeq: 0,
      lastKind: 'none',
      lastMember: 'none',
      fillStyle: 'unset',
      strokeStyle: 'unset',
      globalAlpha: 'unset',
      lineWidth: 'unset',
      globalCompositeOperation: 'unset'
    };
    canvasMetaById[meta.id] = meta;
    if (canvasMetaByObject) {
      canvasMetaByObject.set(canvas, meta);
    }
    return meta;
  }

  function debugCanvasSummary(canvas, source) {
    if (!isCanvasLike(canvas)) {
      return null;
    }
    var meta = getCanvasMeta(canvas);
    var scoreMeta = canvasContentScore(canvas);
    return {
      id: meta ? (meta.id | 0) : -1,
      source: source || 'debug',
      width: (canvas.width | 0),
      height: (canvas.height | 0),
      score: scoreMeta && scoreMeta.score != null ? (scoreMeta.score | 0) : -1,
      signature: scoreMeta && scoreMeta.signature ? String(scoreMeta.signature) : 'none',
      opCount: meta ? (meta.opCount | 0) : 0,
      paintCount: meta ? (meta.paintCount | 0) : 0,
      lastSeq: meta ? (meta.lastSeq | 0) : 0,
      lastPaintSeq: meta ? (meta.lastPaintSeq | 0) : 0,
      lastKind: meta ? String(meta.lastKind || 'none') : 'none',
      lastMember: meta ? String(meta.lastMember || 'none') : 'none',
      fillStyle: meta ? String(meta.fillStyle || 'unset') : 'unset',
      strokeStyle: meta ? String(meta.strokeStyle || 'unset') : 'unset',
      globalAlpha: meta ? String(meta.globalAlpha || 'unset') : 'unset',
      lineWidth: meta ? String(meta.lineWidth || 'unset') : 'unset',
      globalCompositeOperation: meta ? String(meta.globalCompositeOperation || 'unset') : 'unset'
    };
  }

  function noteCanvasOperation(canvas, kind, member, isPaint, assignedValue) {
    var meta = getCanvasMeta(canvas);
    if (!meta) {
      return null;
    }
    meta.opCount++;
    if (kind === 'setter') {
      meta.setterCount++;
    } else if (kind === 'method') {
      meta.methodCount++;
    }
    meta.lastSeq = canvasOpSeq++;
    meta.lastKind = String(kind || 'unknown');
    meta.lastMember = String(member || 'unknown');
    if (kind === 'setter' && canvasStateMemberNames[member]) {
      meta[member] = describeCanvasStateValue(assignedValue);
    }
    if (isPaint) {
      meta.paintCount++;
      meta.lastPaintSeq = meta.lastSeq;
      global.__cn1LastPaintCanvas = canvas;
      global.__cn1LastPaintMember = meta.lastMember;
    }
    return meta;
  }

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
    // Avoid per-element host RPC on large typed-array reads (e.g. ImageData.data).
    // Returning a clone transfers data once to the worker so get(index) executes locally.
    if (typeof Uint8ClampedArray !== 'undefined' && value instanceof Uint8ClampedArray) {
      return new Uint8Array(value);
    }
    if (typeof ArrayBuffer !== 'undefined') {
      if (value instanceof ArrayBuffer) {
        return value.slice(0);
      }
      if (typeof ArrayBuffer.isView === 'function' && ArrayBuffer.isView(value)) {
        return value.slice ? value.slice(0) : value;
      }
    }
    return storeHostRef(value);
  }

  function isCanvasLike(value) {
    return !!(value
      && typeof value.toDataURL === 'function'
      && typeof value.width === 'number'
      && typeof value.height === 'number');
  }

  function noteDrawTarget(receiver, kind, member, assignedValue) {
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
    var isPaint = kind === 'method' && !!drawMethodNames[member];
    if (kind === 'method' && !isPaint) {
      return;
    }
    noteCanvasOperation(canvas, kind, member, isPaint, assignedValue);
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
      // For array-like objects, prefer indexed get/set over native methods
      // because TypedArray.prototype.set(array, offset) has different
      // semantics than the JSO per-element set(index, value).
      if (member === 'get' && args.length === 1 && receiver && typeof receiver.length === 'number') {
        value = receiver[args[0] | 0];
      } else if (member === 'set' && args.length === 2 && receiver && typeof receiver.length === 'number') {
        receiver[args[0] | 0] = args[1];
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
    }
    if (kind === 'getter' && member === 'data' && value && typeof value.length === 'number') {
      if (value.slice) {
        return value.slice(0);
      }
      return Array.prototype.slice.call(value);
    }
    noteDrawTarget(receiver, kind, member, kind === 'setter' && args.length ? args[0] : null);
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
    var regions = [
      [0.5, 0.5],
      [0.2, 0.2], [0.8, 0.2], [0.2, 0.8], [0.8, 0.8],
      [0.5, 0.2], [0.5, 0.8], [0.2, 0.5], [0.8, 0.5]
    ];
    var opaqueCount = 0;
    var signature = 'none';
    var sigHash = 2166136261 >>> 0;
    var sampled = 0;
    var bucketMask = 0;
    var transitionCount = 0;
    var minLuma = 255;
    var maxLuma = 0;
    for (var ri = 0; ri < regions.length; ri++) {
      var rx = regions[ri][0];
      var ry = regions[ri][1];
      var startX = Math.max(0, Math.min(w - sampleW, (((w - sampleW) * rx) | 0)));
      var startY = Math.max(0, Math.min(h - sampleH, (((h - sampleH) * ry) | 0)));
      var img;
      try {
        img = ctx.getImageData(startX, startY, sampleW, sampleH);
      } catch (_err) {
        continue;
      }
      if (!img || !img.data || !img.data.length) {
        continue;
      }
      sampled++;
      var data = img.data;
      var prevLuma = -1;
      var prevAlpha = -1;
      for (var i = 0; i < data.length; i += 4) {
        var r = data[i] | 0;
        var g = data[i + 1] | 0;
        var b = data[i + 2] | 0;
        var a = data[i + 3] | 0;
        if (a > 12) {
          var luma = (((r * 3) + (g * 4) + b) >> 3) | 0;
          opaqueCount++;
          bucketMask |= (1 << ((luma >> 4) & 15));
          if (luma < minLuma) {
            minLuma = luma;
          }
          if (luma > maxLuma) {
            maxLuma = luma;
          }
          if (prevLuma >= 0 && (Math.abs(luma - prevLuma) > 12 || Math.abs(a - prevAlpha) > 12)) {
            transitionCount++;
          }
          prevLuma = luma;
          prevAlpha = a;
        }
        if ((i & 31) === 0) {
          sigHash ^= r;
          sigHash = Math.imul(sigHash, 16777619);
          sigHash ^= g;
          sigHash = Math.imul(sigHash, 16777619);
          sigHash ^= b;
          sigHash = Math.imul(sigHash, 16777619);
          sigHash ^= a;
          sigHash = Math.imul(sigHash, 16777619);
        }
      }
    }
    if (sampled === 0) {
      return null;
    }
    signature = String((sigHash >>> 0).toString(16));
    var distinctBuckets = 0;
    for (var bi = 0; bi < 16; bi++) {
      if ((bucketMask & (1 << bi)) !== 0) {
        distinctBuckets++;
      }
    }
    var variation = maxLuma >= minLuma ? (maxLuma - minLuma) : 0;
    return {
      score: (transitionCount * 8)
        + (Math.max(0, distinctBuckets - 1) * 1024)
        + (Math.max(0, variation - 8) * 4),
      signature: signature
    };
  }

  function getViewportMeta(doc) {
    var win = global.window || global;
    var width = 0;
    var height = 0;
    if (win) {
      width = Math.max(width, win.innerWidth | 0);
      height = Math.max(height, win.innerHeight | 0);
    }
    if (doc) {
      var root = doc.documentElement || null;
      var body = doc.body || null;
      if (root) {
        width = Math.max(width, root.clientWidth | 0);
        height = Math.max(height, root.clientHeight | 0);
      }
      if (body) {
        width = Math.max(width, body.clientWidth | 0);
        height = Math.max(height, body.clientHeight | 0);
      }
    }
    return {
      width: Math.max(0, width | 0),
      height: Math.max(0, height | 0)
    };
  }

  function getCanvasDisplayMeta(canvas, doc, viewportMeta) {
    var meta = {
      domAttached: 0,
      domVisible: 0,
      rootMatch: 0,
      viewportFit: 0,
      rectWidth: 0,
      rectHeight: 0,
      domId: 'none',
      displayAffinity: 0
    };
    if (!canvas || !doc || canvas.nodeType !== 1) {
      return meta;
    }
    var attached = !!(canvas.isConnected || (doc.documentElement && typeof doc.documentElement.contains === 'function' && doc.documentElement.contains(canvas)));
    meta.domAttached = attached ? 1 : 0;
    if (canvas.id) {
      meta.domId = String(canvas.id);
    }
    meta.rootMatch = meta.domId === 'codenameone-canvas' ? 1 : 0;
    var rect = null;
    if (typeof canvas.getBoundingClientRect === 'function') {
      try {
        rect = canvas.getBoundingClientRect();
      } catch (_err) {
        rect = null;
      }
    }
    var rectWidth = rect && isFinite(rect.width) ? Math.max(0, Math.round(rect.width)) : 0;
    var rectHeight = rect && isFinite(rect.height) ? Math.max(0, Math.round(rect.height)) : 0;
    meta.rectWidth = rectWidth;
    meta.rectHeight = rectHeight;
    var visible = attached && rectWidth > 1 && rectHeight > 1;
    if (visible && typeof global.getComputedStyle === 'function') {
      try {
        var style = global.getComputedStyle(canvas);
        if (style) {
          if (style.display === 'none' || style.visibility === 'hidden' || String(style.opacity || '1') === '0') {
            visible = false;
          }
        }
      } catch (_err2) {
        // Ignore style lookup failures in non-DOM hosts.
      }
    }
    meta.domVisible = visible ? 1 : 0;
    var viewportWidth = viewportMeta ? (viewportMeta.width | 0) : 0;
    var viewportHeight = viewportMeta ? (viewportMeta.height | 0) : 0;
    if (viewportWidth > 0 && viewportHeight > 0) {
      var scaleX = rectWidth > 0 ? (rectWidth / viewportWidth) : ((canvas.width | 0) / viewportWidth);
      var scaleY = rectHeight > 0 ? (rectHeight / viewportHeight) : ((canvas.height | 0) / viewportHeight);
      if (scaleX > 0 && scaleY > 0) {
        var scaleBalance = Math.abs(Math.log(scaleX / scaleY));
        var minScale = Math.min(scaleX, scaleY);
        var maxScale = Math.max(scaleX, scaleY);
        if (scaleBalance < 0.35 && minScale >= 0.45 && maxScale <= 4.5) {
          meta.viewportFit = Math.max(1, 1000 - Math.round(scaleBalance * 1200));
        }
      }
    }
    var affinity = 0;
    if (meta.domAttached) {
      affinity += 2000;
    }
    if (meta.domVisible) {
      affinity += 2000;
    }
    if (meta.rootMatch) {
      affinity += 4000;
    }
    affinity += meta.viewportFit | 0;
    meta.displayAffinity = affinity;
    return meta;
  }

  function pickBestCanvasSnapshot(includeDataUrl, previousSignature) {
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
    function sourcePriority(source) {
      switch (source) {
        case 'lastPaint':
          return 5;
        case 'lastDraw':
          return 4;
        case 'hostRefCanvas':
          return 3;
        case 'hostRef':
          return 2;
        case 'dom':
          return 1;
        default:
          return 0;
      }
    }
    function changedFromPrevious(signature, previous) {
      if (!signature || signature === 'none') {
        return 0;
      }
      if (!previous) {
        return 1;
      }
      return signature !== previous ? 1 : 0;
    }
    var previous = previousSignature == null ? '' : String(previousSignature || '');
    var candidates = [];
    var seenCanvases = [];
    pushCanvas(candidates, seenCanvases, global.__cn1LastPaintCanvas || null, 'lastPaint');
    pushCanvas(candidates, seenCanvases, global.__cn1LastDrawCanvas || null, 'lastDraw');
    if (hostRefById) {
      var refKeys = Object.keys(hostRefById);
      for (var rk = 0; rk < refKeys.length; rk++) {
        var refVal = hostRefById[refKeys[rk]];
        if (refVal && refVal.canvas && isCanvasLike(refVal.canvas)) {
          pushCanvas(candidates, seenCanvases, refVal.canvas, 'hostRefCanvas');
          continue;
        }
        if (isCanvasLike(refVal)) {
          pushCanvas(candidates, seenCanvases, refVal, 'hostRef');
        }
      }
    }
    var doc = global.document || (global.window && global.window.document) || null;
    if (doc && typeof doc.querySelectorAll === 'function') {
      var domCanvases = doc.querySelectorAll('canvas');
      if (domCanvases && domCanvases.length) {
        for (var di = 0; di < domCanvases.length; di++) {
          pushCanvas(candidates, seenCanvases, domCanvases[di], 'dom');
        }
      }
    }
    if (!candidates.length) {
      return null;
    }
    var viewportMeta = getViewportMeta(doc);
    var evaluated = [];
    var maxArea = -1;
    for (var i = 0; i < candidates.length; i++) {
      var c = candidates[i].canvas;
      if (!c || (includeDataUrl && typeof c.toDataURL !== 'function')) {
        continue;
      }
      var w = (c.width | 0);
      var h = (c.height | 0);
      var area = w * h;
      var scoreMeta = canvasContentScore(c);
      var canvasMeta = getCanvasMeta(c);
      var displayMeta = getCanvasDisplayMeta(c, doc, viewportMeta);
      var score = scoreMeta && scoreMeta.score != null ? (scoreMeta.score | 0) : -1;
      var signature = scoreMeta && scoreMeta.signature ? String(scoreMeta.signature) : 'none';
      evaluated.push({
        canvas: c,
        index: i,
        source: candidates[i].source || 'unknown',
        sourcePriority: sourcePriority(candidates[i].source || 'unknown'),
        changedFromPrevious: changedFromPrevious(signature, previous),
        area: area,
        score: score,
        signature: signature,
        canvasId: canvasMeta ? canvasMeta.id : -1,
        opCount: canvasMeta ? (canvasMeta.opCount | 0) : 0,
        paintCount: canvasMeta ? (canvasMeta.paintCount | 0) : 0,
        lastSeq: canvasMeta ? (canvasMeta.lastSeq | 0) : 0,
        lastPaintSeq: canvasMeta ? (canvasMeta.lastPaintSeq | 0) : 0,
        lastMember: canvasMeta ? String(canvasMeta.lastMember || 'none') : 'none',
        lastKind: canvasMeta ? String(canvasMeta.lastKind || 'none') : 'none',
        fillStyle: canvasMeta ? String(canvasMeta.fillStyle || 'unset') : 'unset',
        strokeStyle: canvasMeta ? String(canvasMeta.strokeStyle || 'unset') : 'unset',
        globalAlpha: canvasMeta ? String(canvasMeta.globalAlpha || 'unset') : 'unset',
        lineWidth: canvasMeta ? String(canvasMeta.lineWidth || 'unset') : 'unset',
        globalCompositeOperation: canvasMeta ? String(canvasMeta.globalCompositeOperation || 'unset') : 'unset',
        domAttached: displayMeta ? (displayMeta.domAttached | 0) : 0,
        domVisible: displayMeta ? (displayMeta.domVisible | 0) : 0,
        rootMatch: displayMeta ? (displayMeta.rootMatch | 0) : 0,
        viewportFit: displayMeta ? (displayMeta.viewportFit | 0) : 0,
        displayAffinity: displayMeta ? (displayMeta.displayAffinity | 0) : 0,
        domId: displayMeta ? String(displayMeta.domId || 'none') : 'none',
        rectWidth: displayMeta ? (displayMeta.rectWidth | 0) : 0,
        rectHeight: displayMeta ? (displayMeta.rectHeight | 0) : 0,
        width: w,
        height: h
      });
      if (area > maxArea) {
        maxArea = area;
      }
    }
    if (!evaluated.length) {
      return null;
    }
    var minLargeArea = Math.max(65536, Math.floor(maxArea * 0.45));
    var minMeaningfulArea = Math.max(4096, Math.floor(maxArea * 0.005));
    var largePool = [];
    for (var p = 0; p < evaluated.length; p++) {
      if (evaluated[p].area >= minLargeArea) {
        largePool.push(evaluated[p]);
      }
    }
    var pool = largePool.slice();
    for (var q = 0; q < evaluated.length; q++) {
      var mediumCandidate = evaluated[q];
      if (mediumCandidate.area >= minLargeArea) {
        continue;
      }
      if (mediumCandidate.area < minMeaningfulArea) {
        continue;
      }
      if ((mediumCandidate.score | 0) <= 128) {
        continue;
      }
      pool.push(mediumCandidate);
    }
    if (!pool.length) {
      for (var r = 0; r < evaluated.length; r++) {
        if (evaluated[r].area >= minMeaningfulArea) {
          pool.push(evaluated[r]);
        }
      }
    }
    if (!pool.length) {
      pool = evaluated;
    }
    var candidateSummary = [];
    for (var s = 0; s < evaluated.length && s < 8; s++) {
      var summaryEntry = evaluated[s];
      candidateSummary.push(
        String(summaryEntry.index)
        + ':id=' + String(summaryEntry.canvasId | 0)
        + ':' + String(summaryEntry.source || 'unknown')
        + ':' + String(summaryEntry.width | 0) + 'x' + String(summaryEntry.height | 0)
        + ':score=' + String(summaryEntry.score | 0)
        + ':sig=' + String(summaryEntry.signature || 'none')
        + ':paint=' + String(summaryEntry.paintCount | 0)
        + ':ops=' + String(summaryEntry.opCount | 0)
        + ':last=' + String(summaryEntry.lastKind || 'none') + '.' + String(summaryEntry.lastMember || 'none')
        + ':alpha=' + String(summaryEntry.globalAlpha || 'unset')
        + ':fill=' + String(summaryEntry.fillStyle || 'unset')
        + ':stroke=' + String(summaryEntry.strokeStyle || 'unset')
        + ':changed=' + String(summaryEntry.changedFromPrevious | 0)
        + ':aff=' + String(summaryEntry.displayAffinity | 0)
        + ':dom=' + String(summaryEntry.domAttached | 0)
        + ':vis=' + String(summaryEntry.domVisible | 0)
        + ':root=' + String(summaryEntry.rootMatch | 0)
        + ':fit=' + String(summaryEntry.viewportFit | 0)
        + ':large=' + String(summaryEntry.area >= minLargeArea ? 1 : 0)
        + ':keep=' + String(pool.indexOf(summaryEntry) >= 0 ? 1 : 0)
      );
    }
    var best = null;
    var bestArea = -1;
    var bestScore = -1;
    var bestIndex = -1;
    var bestSource = 'none';
    var bestSignature = 'none';
    var bestSourcePriority = -1;
    var bestChangedFromPrevious = -1;
    var bestMeaningful = -1;
    var bestHasPaint = -1;
    var bestPaintCount = -1;
    var bestLastPaintSeq = -1;
    var bestDisplayAffinity = -1;
    for (var j = 0; j < pool.length; j++) {
      var pick = pool[j];
      var pickMeaningful = (pick.score | 0) > 128 ? 1 : 0;
      var pickHasPaint = (pick.paintCount | 0) > 0 ? 1 : 0;
      if (!best
          || pick.displayAffinity > bestDisplayAffinity
          || (pick.displayAffinity === bestDisplayAffinity && pickMeaningful > bestMeaningful)
          || (pick.displayAffinity === bestDisplayAffinity && pickMeaningful === bestMeaningful && pickHasPaint > bestHasPaint)
          || (pick.displayAffinity === bestDisplayAffinity && pickMeaningful === bestMeaningful && pickHasPaint === bestHasPaint && pick.changedFromPrevious > bestChangedFromPrevious)
          || (pick.displayAffinity === bestDisplayAffinity && pickMeaningful === bestMeaningful && pickHasPaint === bestHasPaint && pick.changedFromPrevious === bestChangedFromPrevious && pick.score > bestScore)
          || (pick.displayAffinity === bestDisplayAffinity && pickMeaningful === bestMeaningful && pickHasPaint === bestHasPaint && pick.changedFromPrevious === bestChangedFromPrevious && pick.score === bestScore && pick.paintCount > bestPaintCount)
          || (pick.displayAffinity === bestDisplayAffinity && pickMeaningful === bestMeaningful && pickHasPaint === bestHasPaint && pick.changedFromPrevious === bestChangedFromPrevious && pick.score === bestScore && pick.paintCount === bestPaintCount && pick.lastPaintSeq > bestLastPaintSeq)
          || (pick.displayAffinity === bestDisplayAffinity && pickMeaningful === bestMeaningful && pickHasPaint === bestHasPaint && pick.changedFromPrevious === bestChangedFromPrevious && pick.score === bestScore && pick.paintCount === bestPaintCount && pick.lastPaintSeq === bestLastPaintSeq && pick.sourcePriority > bestSourcePriority)
          || (pick.displayAffinity === bestDisplayAffinity && pickMeaningful === bestMeaningful && pickHasPaint === bestHasPaint && pick.changedFromPrevious === bestChangedFromPrevious && pick.score === bestScore && pick.paintCount === bestPaintCount && pick.lastPaintSeq === bestLastPaintSeq && pick.sourcePriority === bestSourcePriority && pick.area > bestArea)) {
        bestDisplayAffinity = pick.displayAffinity | 0;
        bestMeaningful = pickMeaningful;
        bestHasPaint = pickHasPaint;
        bestChangedFromPrevious = pick.changedFromPrevious;
        bestSourcePriority = pick.sourcePriority;
        bestPaintCount = pick.paintCount | 0;
        bestLastPaintSeq = pick.lastPaintSeq | 0;
        bestScore = pick.score;
        bestArea = pick.area;
        best = pick.canvas;
        bestIndex = pick.index;
        bestSource = pick.source;
        bestSignature = pick.signature;
      }
    }
    if (!best) {
      return null;
    }
    var out = {
      canvasCount: candidates.length,
      canvasConsidered: evaluated.length,
      canvasLargeCount: largePool.length,
      canvasSelectionCount: pool.length,
      canvasMinLargeArea: minLargeArea,
      canvasMinMeaningfulArea: minMeaningfulArea,
      canvasPick: bestIndex,
      canvasArea: bestArea,
      canvasScore: bestScore,
      canvasSource: bestSource,
      canvasSignature: bestSignature,
      canvasDisplayAffinity: bestDisplayAffinity,
      canvasPaintCount: bestPaintCount,
      canvasLastPaintSeq: bestLastPaintSeq,
      canvasCandidatesSummary: candidateSummary.join('|'),
      changedFromPrevious: bestChangedFromPrevious,
      sourcePriority: bestSourcePriority
    };
    var bestEntry = evaluated[bestIndex] || null;
    if (bestEntry) {
      out.canvasPickSummary = String(bestEntry.index)
        + ':id=' + String(bestEntry.canvasId | 0)
        + ':' + String(bestEntry.source || 'unknown')
        + ':' + String(bestEntry.width | 0) + 'x' + String(bestEntry.height | 0)
        + ':score=' + String(bestEntry.score | 0)
        + ':sig=' + String(bestEntry.signature || 'none')
        + ':paint=' + String(bestEntry.paintCount | 0)
        + ':aff=' + String(bestEntry.displayAffinity | 0)
        + ':dom=' + String(bestEntry.domAttached | 0)
        + ':vis=' + String(bestEntry.domVisible | 0)
        + ':root=' + String(bestEntry.rootMatch | 0)
        + ':fit=' + String(bestEntry.viewportFit | 0)
        + ':domId=' + String(bestEntry.domId || 'none')
        + ':rect=' + String(bestEntry.rectWidth | 0) + 'x' + String(bestEntry.rectHeight | 0);
    }
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
    var maxFrames = Math.max(1, Math.min(96, (payload.maxFrames | 0) || 14));
    var stableFrames = Math.max(1, Math.min(6, (payload.stableFrames | 0) || 2));
    var quietFramesRequired = Math.max(1, Math.min(12, (payload.quietFrames | 0) || stableFrames));
    var previousSignature = String(global.__cn1LastScreenshotSignature || '');
    var changed = false;
    var stableCount = 0;
    var lastSignature = '';
    var best = null;
    var startRenderSeq = global.__cn1RenderQueueSeq | 0;
    var seenRenderSeq = startRenderSeq;
    var renderAdvanced = false;
    var quietFrames = 0;
    function chooseBetter(a, b) {
      if (!a) {
        return b;
      }
      if (!b) {
        return a;
      }
      if (!!b.paintedSinceStart !== !!a.paintedSinceStart) {
        return b.paintedSinceStart ? b : a;
      }
      if ((b.canvasDisplayAffinity | 0) !== (a.canvasDisplayAffinity | 0)) {
        return (b.canvasDisplayAffinity | 0) > (a.canvasDisplayAffinity | 0) ? b : a;
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
        var currentRenderSeq = global.__cn1RenderQueueSeq | 0;
        if ((currentRenderSeq | 0) !== (seenRenderSeq | 0)) {
          renderAdvanced = true;
          seenRenderSeq = currentRenderSeq | 0;
          quietFrames = 0;
          stableCount = 0;
          lastSignature = '';
        } else {
          quietFrames++;
        }
        var sample = pickBestCanvasSnapshot(false, previousSignature);
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
          if ((sample.canvasScore | 0) > 0
              && quietFrames >= quietFramesRequired
              && stableCount >= stableFrames) {
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
      diag('SCREENSHOT_START', 'settleQuietFrames', quietFramesRequired);
      diag('SCREENSHOT_START', 'settleChanged', changed ? 1 : 0);
      diag('SCREENSHOT_START', 'settleSig', meta.canvasSignature || 'none');
      diag('SCREENSHOT_START', 'settleScore', meta.canvasScore | 0);
      diag('SCREENSHOT_START', 'settleRenderStartSeq', startRenderSeq | 0);
      diag('SCREENSHOT_START', 'settleRenderEndSeq', seenRenderSeq | 0);
      diag('SCREENSHOT_START', 'settleRenderAdvanced', renderAdvanced ? 1 : 0);
      diag('SCREENSHOT_START', 'settleQuietObserved', quietFrames | 0);
      return {
        changedFromPrevious: changed ? 1 : 0,
        canvasSignature: meta.canvasSignature || 'none',
        canvasScore: meta.canvasScore | 0,
        canvasArea: meta.canvasArea | 0,
        canvasCount: meta.canvasCount | 0,
        canvasPick: meta.canvasPick | 0,
        renderStartSeq: startRenderSeq | 0,
        renderEndSeq: seenRenderSeq | 0,
        renderAdvanced: renderAdvanced ? 1 : 0
      };
    });
  });

  hostBridge.register('__cn1_load_truetype_font__', function(request) {
    // Loads a TrueType/OpenType font into the host document so the browser can
    // resolve 'font-family: <fontName>' at paint time. Called from the worker
    // (HTML5Implementation.loadTrueTypeFont_ via the port.js native binding)
    // because document/FontFace/WebFont are not reachable from the worker.
    //
    // Prefers the CSS FontFace API (covers every browser this port targets)
    // and falls back to <style>@font-face injection + the WebFont loader so a
    // Chromium/Firefox FontFace regression doesn't strand the load path.
    var payload = request || {};
    var fontName = payload.fontName == null ? '' : String(payload.fontName);
    var rawPath = payload.fontUrl == null ? '' : String(payload.fontUrl);
    var fontFormat = payload.fontFormat == null ? 'truetype' : String(payload.fontFormat);
    // Match HTML5Implementation.getResourceAsStream: relative resource names
    // are rooted at "assets/" in the bundle layout, and file paths with a
    // directory portion get collapsed to their basename first. Absolute URLs
    // (data:, http:, /...) pass through untouched.
    var fontUrl = rawPath;
    if (fontUrl) {
      if (!/^(?:data:|https?:|\/)/i.test(fontUrl)) {
        var lastSlash = fontUrl.lastIndexOf('/');
        if (lastSlash >= 0) {
          fontUrl = fontUrl.substring(lastSlash + 1);
        }
        if (fontUrl !== 'icon.png' && fontUrl.indexOf('assets/') !== 0) {
          fontUrl = 'assets/' + fontUrl;
        }
      }
    }
    return new Promise(function(resolve) {
      if (!fontName || !fontUrl) {
        resolve({ loaded: false, reason: 'missing-args' });
        return;
      }
      try {
        // Escape backslashes *before* single quotes so a pathological input
        // like `foo\` can't close the CSS string and smuggle in extra tokens.
        var cssStringEscape = function (s) {
          return String(s).replace(/\\/g, '\\\\').replace(/'/g, "\\'");
        };
        if (typeof FontFace !== 'undefined'
            && typeof document !== 'undefined'
            && document.fonts
            && typeof document.fonts.add === 'function') {
          var descriptor = "url('" + cssStringEscape(fontUrl) + "') format('"
            + cssStringEscape(fontFormat) + "')";
          var ff = new FontFace(fontName, descriptor);
          ff.load().then(function(loaded) {
            try { document.fonts.add(loaded); } catch (_err) {}
            resolve({ loaded: true, path: 'FontFace' });
          }, function(err) {
            if (typeof console !== 'undefined' && typeof console.warn === 'function') {
              console.warn('PARPAR:DIAG:HOST:loadTrueTypeFont:FontFace:fail:fontName=' + fontName
                + ':url=' + fontUrl
                + ':error=' + String(err && err.message ? err.message : err));
            }
            resolve({ loaded: false, path: 'FontFace', error: String(err && err.message ? err.message : err) });
          });
          return;
        }
        if (typeof document !== 'undefined' && document.head) {
          var styleEl = document.createElement('style');
          var escapedName = cssStringEscape(fontName);
          var escapedUrl = cssStringEscape(fontUrl);
          var escapedFormat = cssStringEscape(fontFormat);
          styleEl.appendChild(document.createTextNode(
            "@font-face { font-family: '" + escapedName + "'; "
              + "src: url('" + escapedUrl + "') format('" + escapedFormat + "'); }"
          ));
          document.head.appendChild(styleEl);
        }
        if (typeof WebFont !== 'undefined' && typeof WebFont.load === 'function') {
          WebFont.load({
            custom: { families: [fontName] },
            active: function() { resolve({ loaded: true, path: 'WebFont' }); },
            inactive: function() { resolve({ loaded: false, path: 'WebFont' }); }
          });
        } else {
          setTimeout(function() {
            resolve({ loaded: true, path: 'styleOnly' });
          }, 50);
        }
      } catch (err) {
        resolve({ loaded: false, error: String(err && err.message ? err.message : err) });
      }
    });
  });

  hostBridge.register('__cn1_decode_image_from_url__', function(request) {
    // Creates an <img> on the main thread, sets its src, and awaits
    // HTMLImageElement.decode() so the worker can be handed an
    // already-decoded HTMLImageElement. Without this, the worker's Java
    // code returns from createCrossOriginImageElement as soon as setSrc
    // runs but before the browser has actually fetched/decoded the
    // picture — so NativeImage.isComplete() returns false on the first
    // paint, NativeImage.draw silently no-ops, and theme 9-patch borders
    // / EncodedImage-backed draws end up painting zero bytes.
    var payload = request || {};
    var sourceUrl = payload && payload.sourceUrl != null ? String(payload.sourceUrl) : null;
    var crossOrigin = payload && payload.crossOrigin != null ? String(payload.crossOrigin) : 'anonymous';
    if (!sourceUrl) {
      return null;
    }
    return new Promise(function(resolve) {
      if (typeof document === 'undefined' || !document.createElement) {
        resolve(null);
        return;
      }
      var img;
      try {
        img = document.createElement('img');
      } catch (e) {
        resolve(null);
        return;
      }
      try { img.setAttribute('crossorigin', crossOrigin); } catch (_ignored) {}
      img.src = sourceUrl;
      // HTMLImageElement cannot be structured-cloned back to the worker —
      // wrap it in a host-ref marker (like __cn1_jso_bridge__ does for
      // createElement results) so the worker receives an opaque handle
      // that re-hydrates to the same main-thread element on subsequent
      // bridge calls.
      var settle = function() { resolve(hostResult(img)); };
      // decode() may reject on a decoding error or if the browser has
      // detached the image before it settles; treat either path as
      // "decode done" so the worker still gets the element back and the
      // existing NativeImage error-handling takes over (it'll just paint
      // nothing for broken bytes, matching pre-barrier behaviour).
      if (typeof img.decode === 'function') {
        try {
          img.decode().then(settle, settle);
          return;
        } catch (e) { /* fall through */ }
      }
      // Browsers without HTMLImageElement.decode fall back to the load
      // event (plus a timeout safety net so we don't hang forever on a
      // mis-typed path).
      var done = false;
      var finish = function() {
        if (done) return;
        done = true;
        settle();
      };
      img.addEventListener('load', finish);
      img.addEventListener('error', finish);
      setTimeout(finish, 10000);
    });
  });

  hostBridge.register('__cn1_delay__', function(request) {
    var millis = 0;
    if (request && typeof request === 'object' && request.millis != null) {
      millis = request.millis | 0;
    } else if (request != null) {
      millis = request | 0;
    }
    millis = Math.max(0, Math.min(10000, millis));
    return new Promise(function(resolve) {
      setTimeout(function() {
        resolve(millis);
      }, millis);
    });
  });

  hostBridge.register('__cn1_capture_canvas_png__', function(request) {
    var payload = request || {};
    var includeMeta = !!(payload && payload.includeMeta);
    function captureNow() {
      return pickBestCanvasSnapshot(true, previousSignature);
    }
    var previousSignature = String(global.__cn1LastScreenshotSignature || '');
    var baseline = pickBestCanvasSnapshot(false, previousSignature) || null;
    var baselinePaintSeq = baseline ? (baseline.canvasLastPaintSeq | 0) : 0;
    var baselinePaintCount = baseline ? (baseline.canvasPaintCount | 0) : 0;
    var attempts = 24;
    var best = null;
    var startRenderSeq = global.__cn1RenderQueueSeq | 0;
    var seenRenderSeq = startRenderSeq;
    var renderAdvanced = false;
    var quietFrames = 0;
    var quietFramesRequired = 3;
    function chooseBetter(a, b) {
      if (!a) {
        return b;
      }
      if (!b) {
        return a;
      }
      if ((b.canvasDisplayAffinity | 0) !== (a.canvasDisplayAffinity | 0)) {
        return (b.canvasDisplayAffinity | 0) > (a.canvasDisplayAffinity | 0) ? b : a;
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
        var currentRenderSeq = global.__cn1RenderQueueSeq | 0;
        if ((currentRenderSeq | 0) !== (seenRenderSeq | 0)) {
          renderAdvanced = true;
          seenRenderSeq = currentRenderSeq | 0;
          quietFrames = 0;
        } else {
          quietFrames++;
        }
        var sample = captureNow();
        if (sample) {
          sample.attempt = index;
          sample.changedFromPrevious = !!(sample.canvasSignature && sample.canvasSignature !== previousSignature);
          sample.paintedSinceStart =
            ((sample.canvasLastPaintSeq | 0) > (baselinePaintSeq | 0))
            || ((sample.canvasPaintCount | 0) > (baselinePaintCount | 0));
          best = chooseBetter(best, sample);
          if (quietFrames >= quietFramesRequired
              && (sample.canvasScore | 0) > 0
              && (sample.paintedSinceStart || renderAdvanced || sample.changedFromPrevious)) {
            return sample;
          }
          if ((sample.canvasScore | 0) <= 0 && !sample.paintedSinceStart) {
            // Keep waiting when we are still looking at the same blank frame.
            sample = null;
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
        global.__cn1LastCaptureMeta = null;
        return includeMeta ? {
          dataUrl: '',
          canvasScore: -1,
          canvasLastPaintSeq: baselinePaintSeq | 0,
          canvasPaintedSinceStart: 0
        } : '';
      }
      global.__cn1LastScreenshotSignature = result.canvasSignature || '';
      diag('SCREENSHOT_START', 'canvasCount', result.canvasCount);
      diag('SCREENSHOT_START', 'canvasConsidered', result.canvasConsidered | 0);
      diag('SCREENSHOT_START', 'canvasLargeCount', result.canvasLargeCount | 0);
      diag('SCREENSHOT_START', 'canvasSelectionCount', result.canvasSelectionCount | 0);
      diag('SCREENSHOT_START', 'canvasMinLargeArea', result.canvasMinLargeArea | 0);
      diag('SCREENSHOT_START', 'canvasMinMeaningfulArea', result.canvasMinMeaningfulArea | 0);
      diag('SCREENSHOT_START', 'canvasPick', result.canvasPick);
      diag('SCREENSHOT_START', 'canvasArea', result.canvasArea);
      diag('SCREENSHOT_START', 'canvasScore', result.canvasScore);
      diag('SCREENSHOT_START', 'canvasDisplayAffinity', result.canvasDisplayAffinity | 0);
      diag('SCREENSHOT_START', 'canvasSource', result.canvasSource || 'unknown');
      diag('SCREENSHOT_START', 'canvasSig', result.canvasSignature || 'none');
      diag('SCREENSHOT_START', 'canvasBaselinePaintSeq', baselinePaintSeq | 0);
      diag('SCREENSHOT_START', 'canvasBaselinePaintCount', baselinePaintCount | 0);
      diag('SCREENSHOT_START', 'canvasPaintCount', result.canvasPaintCount | 0);
      diag('SCREENSHOT_START', 'canvasLastPaintSeq', result.canvasLastPaintSeq | 0);
      diag('SCREENSHOT_START', 'canvasPaintedSinceStart', result.paintedSinceStart ? 1 : 0);
      diag('SCREENSHOT_START', 'canvasCandidates', result.canvasCandidatesSummary || 'none');
      diag('SCREENSHOT_START', 'canvasPickSummary', result.canvasPickSummary || 'none');
      diag('SCREENSHOT_START', 'canvasRenderStartSeq', startRenderSeq | 0);
      diag('SCREENSHOT_START', 'canvasRenderEndSeq', seenRenderSeq | 0);
      diag('SCREENSHOT_START', 'canvasRenderAdvanced', renderAdvanced ? 1 : 0);
      diag('SCREENSHOT_START', 'canvasQuietObserved', quietFrames | 0);
      diag('SCREENSHOT_START', 'attempt', result.attempt | 0);
      diag('SCREENSHOT_START', 'changed', result.changedFromPrevious ? 1 : 0);
      diag('SCREENSHOT_START', 'pngLen', String(result.dataUrl || '').length);
      global.__cn1LastCaptureMeta = {
        dataUrl: String(result.dataUrl || ''),
        canvasScore: result.canvasScore | 0,
        canvasLastPaintSeq: result.canvasLastPaintSeq | 0,
        canvasPaintedSinceStart: result.paintedSinceStart ? 1 : 0,
        canvasSignature: result.canvasSignature || 'none'
      };
      if (includeMeta) {
        return global.__cn1LastCaptureMeta;
      }
      return String(result.dataUrl || '');
    });
  });

  hostBridge.register('__cn1_debug_list_canvases__', function() {
    var out = [];
    var keys = Object.keys(canvasMetaById);
    for (var i = 0; i < keys.length; i++) {
      var meta = canvasMetaById[keys[i]];
      if (!meta || !meta.canvas || !isCanvasLike(meta.canvas)) {
        continue;
      }
      var summary = debugCanvasSummary(meta.canvas, 'meta');
      if (summary) {
        out.push(summary);
      }
    }
    out.sort(function(a, b) {
      return (a.id | 0) - (b.id | 0);
    });
    return out;
  });

  hostBridge.register('__cn1_debug_capture_canvas_by_id__', function(request) {
    var id = request;
    if (request && typeof request === 'object' && request.id != null) {
      id = request.id;
    }
    id = id | 0;
    var meta = canvasMetaById[id];
    if (!meta || !meta.canvas || !isCanvasLike(meta.canvas) || typeof meta.canvas.toDataURL !== 'function') {
      return '';
    }
    return String(meta.canvas.toDataURL('image/png') || '');
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
      // Detect app lifecycle start from worker-side log messages so the
      // main-thread cn1Started flag is set even when @JSBody runs in the
      // worker where window === self.
      var msg = String(data.message);
      if (msg.indexOf('CN1JS:RenderQueue.flush ops=') >= 0) {
        global.__cn1RenderQueueSeq = (global.__cn1RenderQueueSeq | 0) + 1;
        global.__cn1RenderQueueLastType = 'flush';
        global.__cn1RenderQueueLastLog = msg;
      } else if (msg.indexOf('CN1JS:RenderQueue.drain ops=') >= 0) {
        global.__cn1RenderQueueSeq = (global.__cn1RenderQueueSeq | 0) + 1;
        global.__cn1RenderQueueLastType = 'drain';
        global.__cn1RenderQueueLastLog = msg;
      }
      if (!global.cn1Started && msg.indexOf('CN1JS:') >= 0 && msg.indexOf('.runApp') >= 0) {
        global.cn1Started = true;
      }
    }
  }

  function installWorkerMode() {
    log('worker-mode');
    diag('BOOT', 'bridgeMode', 'worker');
    var workerUrl = 'worker.js';
    if (global.location && global.location.search) {
      workerUrl += String(global.location.search);
    }
    var worker = new Worker(workerUrl);
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
    // devicePixelRatio lives only on the main-thread window — workers see
    // `self.devicePixelRatio` as undefined and fall back to 1.0, which in
    // turn drives CN1's density picker to DENSITY_MEDIUM on real retina
    // devices. Ship the current ratio over with the boot message so the
    // worker can surface it through getDevicePixelRatio() without needing
    // a round-trip host-bridge call on every read.
    var dpr = 1.0;
    try {
      if (global.devicePixelRatio && global.devicePixelRatio > 0) {
        dpr = Number(global.devicePixelRatio);
      }
    } catch (e) { /* ignore */ }
    worker.postMessage({
      type: 'start',
      locationSearch: (global.location && global.location.search) ? String(global.location.search) : '',
      devicePixelRatio: dpr
    });
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', global.startParparVmApp);
  } else {
    global.startParparVmApp();
  }
  diag('BOOT', 'bridge', 'loaded');
})(self);
