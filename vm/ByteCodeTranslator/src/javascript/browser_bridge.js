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
    // Gate browser-bridge PARPAR:* log entries behind the same diagEnabled
    // toggle (``?parparDiag=1``) that already gates diag(). Without this,
    // every production page load emitted PARPAR:worker-mode /
    // PARPAR:startParparVmApp / PARPAR:appStarter-present regardless of
    // context. Tests that *want* these — the Playwright harness passes
    // parparDiag=1 — still get them.
    if (!diagEnabled) {
      return;
    }
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
      // Fire-and-forget request: the worker passed ``__cn1_no_response``
      // because the Java caller is a void method whose green thread
      // shouldn't block on a HOST_CALLBACK round-trip. Run the handler
      // and don't post a callback. (For JSO bridge requests the flag
      // lives on ``args[0].__cn1_no_response``; ``__cn1_jso_bridge__``
      // is the only handler that uses it, and the per-canvas-op flood
      // it eliminates was what starved ``self.onmessage`` for incoming
      // pointer events during a Dialog modal.)
      var noResponse = !!(args && args[0] && args[0].__cn1_no_response);
      if (!handler) {
        diag('FIRST_FAILURE', 'category', 'host_call_unhandled');
        diag('FIRST_FAILURE', 'symbol', symbol);
        if (!noResponse) {
          postHostCallback(target, id, null, 'Unhandled host call ' + symbol);
        }
        return;
      }
      try {
        normalizeHostResult(handler.apply(null, args || []), function(value, err) {
          if (noResponse && err == null) {
            return;
          }
          postHostCallback(target, id, value, err);
        });
      } catch (err) {
        // Errors must surface even for fire-and-forget, otherwise a bad
        // op silently corrupts the canvas state with no signal to the
        // worker that the chain went off the rails.
        postHostCallback(target, id, null, err);
      }
    }
  };

  // ---- Native interface dispatch -------------------------------------------------
  // Codename One NativeInterface calls arrive here (on the MAIN thread) from the
  // worker via the generated <Iface>Impl -> NativeInterfaceBridge.call* host-hooks.
  // We look up the developer's JS implementation in cn1_native_interfaces (the
  // registry the stub self-registers into, populated on the main thread by the
  // <script>-loaded stub) and invoke it with the trailing callback, returning a
  // Promise so the worker resumes with the result once callback.complete fires.
  function cn1InvokeNativeInterface(iface, method, args) {
    var registry = global.cn1_native_interfaces
            || (global.window && global.window.cn1_native_interfaces);
    var impl = registry ? registry[iface] : null;
    if (!impl) {
      return Promise.reject(new Error('No native interface implementation registered for ' + iface));
    }
    var fn = impl[method];
    if (typeof fn !== 'function') {
      return Promise.reject(new Error('Native interface ' + iface + ' has no implementation for ' + method));
    }
    var callArgs = [];
    if (args != null) {
      for (var i = 0; i < args.length; i++) {
        callArgs.push(args[i]);
      }
    }
    return new Promise(function(resolve, reject) {
      var settled = false;
      var callback = {
        complete: function(value) {
          if (settled) return;
          settled = true;
          resolve(value === undefined ? null : value);
        },
        error: function(err) {
          if (settled) return;
          settled = true;
          reject(err instanceof Error ? err : new Error(err == null ? 'native interface error' : String(err)));
        }
      };
      callArgs.push(callback);
      try {
        fn.apply(impl, callArgs);
      } catch (e) {
        if (!settled) {
          settled = true;
          reject(e);
        }
      }
    });
  }

  // One handler per NativeInterfaceBridge.call* native symbol (the suffix encodes
  // the bridge method + its (String, String, Object[]) signature and return type;
  // void has no _R_). Dispatch is identical across return types -- the worker side
  // coerces the resolved value to the declared Java type.
  var __cn1NativeInterfaceBridgeSymbols = [
    'callBoolean_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_boolean',
    'callInt_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_int',
    'callLong_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_long',
    'callDouble_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_double',
    'callFloat_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_float',
    'callByte_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_byte',
    'callShort_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_short',
    'callChar_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_char',
    'callString_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_java_lang_String',
    'callObject_java_lang_String_java_lang_String_java_lang_Object_1ARRAY_R_java_lang_Object',
    'callVoid_java_lang_String_java_lang_String_java_lang_Object_1ARRAY'
  ];
  for (var __cn1NiIdx = 0; __cn1NiIdx < __cn1NativeInterfaceBridgeSymbols.length; __cn1NiIdx++) {
    (function(suffix) {
      var symbol = 'cn1_com_codename1_impl_platform_js_NativeInterfaceBridge_' + suffix;
      hostBridge.register(symbol, function(iface, method, args) {
        return cn1InvokeNativeInterface(iface, method, args);
      });
    })(__cn1NativeInterfaceBridgeSymbols[__cn1NiIdx]);
  }

  var hostRefNextId = 1;
  var hostRefById = {};
  var hostRefByObject = (typeof WeakMap === 'function') ? new WeakMap() : null;
  // Count of host refs the owning-object finalizer has released (see
  // releaseHostRefs); retained as a lightweight liveness counter.
  var __cn1HostRefReleased = 0;
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

  // Singletons that must never be released even if the worker reports them
  // dead: their wrappers are cached for the life of the page. Defensive -- the
  // display canvas is held by a long-lived Java field so its wrapper never
  // dies, but guard it anyway.
  function isProtectedHostRef(value) {
    // Callers (releaseHostRefs) already screen out null before reaching here.
    if (value === global || value === global.window) {
      return true;
    }
    var doc = global.document || (global.window && global.window.document);
    if (doc && (value === doc || value === doc.body
        || value === doc.documentElement || value === doc.head)) {
      return true;
    }
    if (value.id === 'codenameone-canvas') {
      return true;
    }
    return false;
  }

  // Drop the host refs the worker's Java-side finalizer reported dead. Each id
  // belongs to a front-end resource (an image's backing canvas / HTMLImageElement)
  // whose owning Java image has been GC'd -- the owner was the sole holder of
  // the id (see parparvm_runtime.js registerNativeResource), so the resource is
  // genuinely unreachable and safe to evict, freeing the element and its
  // multi-MB backing store. We release whatever id the owner owned (canvas or
  // image); the only guard is the never-release singleton allowlist
  // (window/document/body/the display canvas), which a real image owner can
  // never legitimately report.
  function releaseHostRefs(ids) {
    if (!ids || !ids.length || !hostRefById) {
      return;
    }
    for (var i = 0; i < ids.length; i++) {
      var id = ids[i];
      var value = hostRefById[id];
      if (value == null || isProtectedHostRef(value)) {
        continue;
      }
      delete hostRefById[id];
      __cn1HostRefReleased++;
      if (hostRefByObject && typeof hostRefByObject.delete === 'function') {
        try {
          hostRefByObject.delete(value);
        } catch (weakErr) {
          void weakErr;
        }
      }
    }
  }

  // Cache of worker-callback proxy functions keyed by the callback ID the
  // worker minted. addEventListener/removeEventListener parity needs the
  // *same* real function on both sides of the call, so we memoise here.
  var workerCallbackProxies = Object.create(null);

  // Serialise the fields of a DOM Event the worker-side EventListener
  // wrappers in port.js actually read. Everything here is either a
  // primitive or a host-ref marker so it round-trips through postMessage
  // without losing information. We extend this as more event types show
  // up in real user code; the bulk (mouse/key/wheel/resize/popstate) is
  // covered below.
  function serializeEventForWorker(evt) {
    if (evt == null || typeof evt !== 'object') {
      return evt;
    }
    var out = {
      type: evt.type || '',
      bubbles: !!evt.bubbles,
      cancelable: !!evt.cancelable,
      defaultPrevented: !!evt.defaultPrevented,
      eventPhase: evt.eventPhase | 0,
      timeStamp: +evt.timeStamp || 0
    };
    if ('clientX' in evt) out.clientX = +evt.clientX || 0;
    if ('clientY' in evt) out.clientY = +evt.clientY || 0;
    if ('pageX'   in evt) out.pageX   = +evt.pageX   || 0;
    if ('pageY'   in evt) out.pageY   = +evt.pageY   || 0;
    if ('screenX' in evt) out.screenX = +evt.screenX || 0;
    if ('screenY' in evt) out.screenY = +evt.screenY || 0;
    if ('button'  in evt) out.button  = evt.button  | 0;
    if ('buttons' in evt) out.buttons = evt.buttons | 0;
    if ('detail'  in evt) out.detail  = evt.detail  | 0;
    if ('deltaX'  in evt) out.deltaX  = +evt.deltaX || 0;
    if ('deltaY'  in evt) out.deltaY  = +evt.deltaY || 0;
    if ('deltaZ'  in evt) out.deltaZ  = +evt.deltaZ || 0;
    if ('deltaMode' in evt) out.deltaMode = evt.deltaMode | 0;
    if ('key'     in evt) out.key     = evt.key == null ? '' : String(evt.key);
    if ('code'    in evt) out.code    = evt.code == null ? '' : String(evt.code);
    if ('keyCode' in evt) out.keyCode = evt.keyCode | 0;
    if ('which'   in evt) out.which   = evt.which   | 0;
    if ('charCode' in evt) out.charCode = evt.charCode | 0;
    if ('shiftKey' in evt) out.shiftKey = !!evt.shiftKey;
    if ('ctrlKey'  in evt) out.ctrlKey  = !!evt.ctrlKey;
    if ('altKey'   in evt) out.altKey   = !!evt.altKey;
    if ('metaKey'  in evt) out.metaKey  = !!evt.metaKey;
    if ('repeat'   in evt) out.repeat   = !!evt.repeat;
    // preventDefault / stopPropagation are fire-and-forget from the worker
    // side (we eagerly call them on the main-thread event just in case).
    // touches arrays are serialised shallow — most user code reads the
    // first touch's clientX/Y which is the same as the top-level fields
    // except on real multi-touch, but the port.js shims use the flat
    // fields already.
    if (evt.target && typeof storeHostRef === 'function') {
      out.target = storeHostRef(evt.target);
    }
    if (evt.currentTarget && typeof storeHostRef === 'function') {
      out.currentTarget = storeHostRef(evt.currentTarget);
    }
    // preventDefault / stopPropagation stubs are re-attached on the
    // worker side (structured-clone postMessage cannot clone functions),
    // see parparvm_runtime.js `worker-callback` message handling.
    return out;
  }

  // Main-thread proxy for a worker-side callback. When the browser fires
  // a DOM event, we postMessage { type: 'worker-callback', callbackId,
  // args: [<serialised event>] } back to the worker, which runs the
  // function that originally produced this ID. We preventDefault/stop
  // propagation side effects happen on the main-thread event before the
  // message round-trip, because the worker may not reply synchronously
  // and a deferred preventDefault would miss the browser's dispatch
  // window. Apps that depend on conditional preventDefault need to set
  // it from the native host-bridge path instead.
  function makeWorkerCallback(callbackId) {
    if (workerCallbackProxies[callbackId]) {
      return workerCallbackProxies[callbackId];
    }
    var fn = function(event) {
      var target = global.__parparWorker;
      if (!target || typeof target.postMessage !== 'function') {
        return;
      }
      var payload;
      try {
        payload = serializeEventForWorker(event);
      } catch (err) {
        payload = null;
      }
      try {
        target.postMessage({
          type: 'worker-callback',
          callbackId: callbackId,
          args: [payload]
        });
      } catch (err) {
        diag('FIRST_FAILURE', 'category', 'worker_callback_post_failed');
        diag('FIRST_FAILURE', 'message', err && err.message ? err.message : String(err));
      }
    };
    fn.__cn1WorkerCallbackId = callbackId;
    workerCallbackProxies[callbackId] = fn;
    return fn;
  }

  function mapHostArgs(args) {
    var out = [];
    var list = args || [];
    for (var i = 0; i < list.length; i++) {
      var arg = list[i];
      if (arg && typeof arg === 'object' && typeof arg.__cn1WorkerCallback === 'number') {
        out.push(makeWorkerCallback(arg.__cn1WorkerCallback));
      } else {
        out.push(resolveHostRef(arg));
      }
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

  // Install a `writeBuffer(arr)` method on `ImageData.prototype` so the
  // worker can copy bytes into the live host-side `imageData.data` buffer in
  // one shot. The worker can't write to `imageData.data` from its side
  // because `hostResult` clones any returned `Uint8ClampedArray` to a fresh
  // worker-local view (read perf optimization, see line ~485) — so a worker
  // call like `((Uint8ClampedArraySetter)d.getData()).set(arr)` writes into
  // the clone, not the original. `putImageData(d)` then sees zeros. This
  // helper sidesteps the clone: the bridge call lands on `ImageData` itself
  // (resolved via host-ref), and `this.data.set(host_arr)` runs entirely on
  // the host where `this.data` is the live buffer.
  if (typeof ImageData !== 'undefined' && ImageData.prototype && !ImageData.prototype.writeArgbBuffer) {
    var __waFn = function(argb, offset, width, height) {
      // ``argb`` is a Java int[] cloned via postMessage. It survives as an
      // array-like with ``.length`` and integer-indexed entries. Unpack each
      // 32-bit ARGB word into RGBA bytes directly into ``this.data`` — that
      // buffer is live on host, so ``putImageData`` will see what we wrote.
      var data = this.data;
      var off = offset | 0;
      var w = width | 0;
      var h = height | 0;
      var pixelCount = w * h;
      var dstLen = data.length;
      var maxPixels = (dstLen / 4) | 0;
      if (pixelCount > maxPixels) pixelCount = maxPixels;
      for (var i = 0; i < pixelCount; i++) {
        var argbWord = argb[off + i] | 0;
        var di = i * 4;
        data[di] = (argbWord >>> 16) & 0xFF;
        data[di + 1] = (argbWord >>> 8) & 0xFF;
        data[di + 2] = argbWord & 0xFF;
        data[di + 3] = (argbWord >>> 24) & 0xFF;
      }
    };
    try {
      Object.defineProperty(ImageData.prototype, 'writeArgbBuffer', {
        value: __waFn,
        writable: true, configurable: true, enumerable: false
      });
    } catch (_e) {
      try { ImageData.prototype.writeArgbBuffer = __waFn; } catch (_e2) {}
    }
  }

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
        } else if (typeof receiver === 'function') {
          // Functional-interface (SAM) receivers — see parparvm_runtime.js
          // ``invokeJsoBridge`` for the full rationale. Plain JS function
          // wrapped as e.g. an EventListener / Runnable / SuccessCallback
          // gets dispatched by calling the function itself; ``handleEvent``
          // / ``run`` / ``onSuccess`` aren't properties of a function
          // value. Without this fallback, every ``addEventListener(type,
          // fn)`` whose listener round-trips back into the worker as a
          // SAM call fails with ``Missing JS member handleEvent``.
          value = receiver.apply(null, args);
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
    // SMOKING GUN DIAG: when a getter returns a number for document/getContext
    // (the canvasContextWipe leak path), log the receiver shape so we can
    // identify whether the receiver is the actual Window/Canvas or has been
    // corrupted/wrong.
    if (typeof value === 'number'
        && (member === 'document' || member === 'getContext')) {
      if (!global.__cn1NumberLeakLogged) global.__cn1NumberLeakLogged = 0;
      if (global.__cn1NumberLeakLogged < 5) {
        global.__cn1NumberLeakLogged++;
        try {
          var protoName = '<none>';
          try {
            var proto = Object.getPrototypeOf(receiver);
            protoName = proto && proto.constructor && proto.constructor.name || '<unknown>';
          } catch (_e1) {}
          // receiver is guaranteed non-null here -- the earlier null-check
          // at the top of the bridge handler throws "Missing host receiver
          // for JSO bridge" before we ever reach this diag block.
          var receiverDesc = 'unknown';
          try {
            if (receiver === global.window) receiverDesc = 'global.window';
            else if (global.window && receiver === global.window.document) receiverDesc = 'global.window.document';
            else if (typeof receiver === 'object' && typeof receiver.tagName === 'string') receiverDesc = 'element:' + receiver.tagName;
            else receiverDesc = String(receiver).slice(0, 60);
          } catch (_e2) {}
          diag('NUMBER_LEAK', 'member', String(member));
          diag('NUMBER_LEAK', 'kind', String(kind));
          diag('NUMBER_LEAK', 'value', String(value));
          diag('NUMBER_LEAK', 'receiverTypeof', typeof receiver);
          diag('NUMBER_LEAK', 'receiverProto', protoName);
          diag('NUMBER_LEAK', 'receiverDesc', receiverDesc);
          diag('NUMBER_LEAK', 'receiverHasDocument', String(typeof receiver.document));
          diag('NUMBER_LEAK', 'receiverHasGetContext', String(typeof receiver.getContext));
        } catch (_e) {}
      }
    }
    return hostResult(value);
  });

  // ===================================================================
  // SURFACE BRIDGE  (surface-id render model)
  // -------------------------------------------------------------------
  // The worker (Java) side holds opaque, WORKER-ASSIGNED surface ids -- never
  // canvas / CanvasRenderingContext2D host-refs. It records draw calls into a
  // flat command stream (see SurfaceCommandRecorder.java) and flushes them
  // fire-and-forget. This host keeps the id->{canvas,ctx} table and replays the
  // stream. Only ``__cn1_surface_read__`` (getRGB) ever returns pixels. The
  // opcodes below MUST mirror SurfaceCommandRecorder.OP_* exactly.
  var SURF = {
    SAVE: 1, RESTORE: 2, SCALE: 3, ROTATE: 4, TRANSLATE: 5, TRANSFORM: 6,
    SET_TRANSFORM: 7, SET_GLOBAL_ALPHA: 8, SET_GCO: 9, SET_FILL_COLOR: 10,
    SET_STROKE_COLOR: 11, SET_LINE_WIDTH: 12, SET_LINE_CAP: 13, SET_LINE_JOIN: 14,
    SET_MITER_LIMIT: 15, SET_FONT: 16, SET_TEXT_ALIGN: 17, SET_TEXT_BASELINE: 18,
    SET_SHADOW_COLOR: 19, SET_SHADOW_BLUR: 20, SET_SHADOW_OFFX: 21, SET_SHADOW_OFFY: 22,
    SET_FILTER: 23, CLEAR_RECT: 24, FILL_RECT: 25, STROKE_RECT: 26, BEGIN_PATH: 27,
    CLOSE_PATH: 28, MOVE_TO: 29, LINE_TO: 30, QUAD_TO: 31, BEZIER_TO: 32, ARC: 33,
    ARC_TO: 34, ELLIPSE: 35, RECT: 36, FILL: 37, STROKE: 38, CLIP: 39,
    FILL_TEXT: 40, STROKE_TEXT: 41, SET_LINE_DASH_OFFSET: 42, SET_LINE_DASH: 43,
    CREATE_LINEAR_GRADIENT: 50, CREATE_RADIAL_GRADIENT: 51, ADD_COLOR_STOP: 52,
    SET_FILL_GRADIENT: 53, SET_STROKE_GRADIENT: 54, CREATE_PATTERN: 55, SET_FILL_PATTERN: 56,
    CREATE_PATTERN_SURFACE: 57,
    DRAW_IMAGE_XY: 60, DRAW_IMAGE_XYWH: 61, DRAW_IMAGE_SRCDST: 62,
    BLIT_SURFACE_XY: 70, BLIT_SURFACE_XYWH: 71, BLIT_SURFACE_SRCDST: 72
  };
  // The display surface id. Mirrors HTML5Implementation.DISPLAY_SURFACE_ID.
  var SURF_DISPLAY_ID = 1;
  var surfaceTable = {};

  function surfaceImageSource(marker) {
    // A drawImage source: either a host-ref marker (a loaded image / canvas
    // that stays a host-side resource) or already a real element.
    var resolved = resolveHostRef(marker);
    if (resolved != null) {
      return resolved;
    }
    return marker;
  }

  function getSurface(id, createW, createH) {
    var s = surfaceTable[id];
    if (s) {
      return s;
    }
    if (id === SURF_DISPLAY_ID) {
      // Bind the display surface to the real output canvas lazily.
      var doc = global.document || (global.window && global.window.document);
      var out = doc ? doc.getElementById('codenameone-canvas') : null;
      if (!out) {
        return null;
      }
      s = { canvas: out, ctx: out.getContext('2d') };
      surfaceTable[id] = s;
      return s;
    }
    if (createW == null) {
      return null;
    }
    var d2 = global.document || (global.window && global.window.document);
    if (!d2 || !d2.createElement) {
      return null;
    }
    var cv = d2.createElement('canvas');
    cv.width = createW | 0;
    cv.height = createH | 0;
    s = { canvas: cv, ctx: cv.getContext('2d') };
    surfaceTable[id] = s;
    return s;
  }

  // Replay one command stream (opcodes + nums + objs) onto ``ctx``.
  function replaySurfaceCommands(ctx, ops, opCount, nums, objs) {
    var ni = 0; // num cursor
    var oi = 0; // obj cursor
    var curGradient = null;
    var curPattern = null;
    for (var k = 0; k < opCount; k++) {
      var code = ops[k];
      switch (code) {
        case SURF.SAVE: ctx.save(); break;
        case SURF.RESTORE: ctx.restore(); break;
        case SURF.SCALE: ctx.scale(nums[ni++], nums[ni++]); break;
        case SURF.ROTATE: ctx.rotate(nums[ni++]); break;
        case SURF.TRANSLATE: ctx.translate(nums[ni++], nums[ni++]); break;
        case SURF.TRANSFORM: ctx.transform(nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++]); break;
        case SURF.SET_TRANSFORM: ctx.setTransform(nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++]); break;
        case SURF.SET_GLOBAL_ALPHA: ctx.globalAlpha = nums[ni++]; break;
        case SURF.SET_GCO: ctx.globalCompositeOperation = objs[oi++]; break;
        case SURF.SET_FILL_COLOR: ctx.fillStyle = objs[oi++]; break;
        case SURF.SET_STROKE_COLOR: ctx.strokeStyle = objs[oi++]; break;
        case SURF.SET_LINE_WIDTH: ctx.lineWidth = nums[ni++]; break;
        case SURF.SET_LINE_CAP: ctx.lineCap = objs[oi++]; break;
        case SURF.SET_LINE_JOIN: ctx.lineJoin = objs[oi++]; break;
        case SURF.SET_MITER_LIMIT: ctx.miterLimit = nums[ni++]; break;
        case SURF.SET_FONT: ctx.font = objs[oi++]; break;
        case SURF.SET_TEXT_ALIGN: ctx.textAlign = objs[oi++]; break;
        case SURF.SET_TEXT_BASELINE: ctx.textBaseline = objs[oi++]; break;
        case SURF.SET_SHADOW_COLOR: ctx.shadowColor = objs[oi++]; break;
        case SURF.SET_SHADOW_BLUR: ctx.shadowBlur = nums[ni++]; break;
        case SURF.SET_SHADOW_OFFX: ctx.shadowOffsetX = nums[ni++]; break;
        case SURF.SET_SHADOW_OFFY: ctx.shadowOffsetY = nums[ni++]; break;
        case SURF.SET_FILTER: try { ctx.filter = objs[oi++]; } catch (_ef) {} break;
        case SURF.CLEAR_RECT: ctx.clearRect(nums[ni++], nums[ni++], nums[ni++], nums[ni++]); break;
        case SURF.FILL_RECT: ctx.fillRect(nums[ni++], nums[ni++], nums[ni++], nums[ni++]); break;
        case SURF.STROKE_RECT: ctx.strokeRect(nums[ni++], nums[ni++], nums[ni++], nums[ni++]); break;
        case SURF.BEGIN_PATH: ctx.beginPath(); break;
        case SURF.CLOSE_PATH: ctx.closePath(); break;
        case SURF.MOVE_TO: ctx.moveTo(nums[ni++], nums[ni++]); break;
        case SURF.LINE_TO: ctx.lineTo(nums[ni++], nums[ni++]); break;
        case SURF.QUAD_TO: ctx.quadraticCurveTo(nums[ni++], nums[ni++], nums[ni++], nums[ni++]); break;
        case SURF.BEZIER_TO: ctx.bezierCurveTo(nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++]); break;
        case SURF.ARC: {
          var ax = nums[ni++], ay = nums[ni++], ar = nums[ni++], a0 = nums[ni++], a1 = nums[ni++], accw = nums[ni++];
          ctx.arc(ax, ay, ar, a0, a1, accw !== 0);
          break;
        }
        case SURF.ARC_TO: ctx.arcTo(nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++]); break;
        case SURF.ELLIPSE: ctx.ellipse(nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++]); break;
        case SURF.RECT: ctx.rect(nums[ni++], nums[ni++], nums[ni++], nums[ni++]); break;
        case SURF.FILL: ctx.fill(); break;
        case SURF.STROKE: ctx.stroke(); break;
        case SURF.CLIP: ctx.clip(); break;
        case SURF.FILL_TEXT: {
          var fx = nums[ni++], fy = nums[ni++], fmw = nums[ni++], ftext = objs[oi++];
          if (fmw >= 0) { ctx.fillText(ftext, fx, fy, fmw); } else { ctx.fillText(ftext, fx, fy); }
          break;
        }
        case SURF.STROKE_TEXT: {
          var sx0 = nums[ni++], sy0 = nums[ni++], smw = nums[ni++], stext = objs[oi++];
          if (smw >= 0) { ctx.strokeText(stext, sx0, sy0, smw); } else { ctx.strokeText(stext, sx0, sy0); }
          break;
        }
        case SURF.SET_LINE_DASH_OFFSET: ctx.lineDashOffset = nums[ni++]; break;
        case SURF.SET_LINE_DASH: {
          var dn = nums[ni++] | 0, seg = new Array(dn);
          for (var di = 0; di < dn; di++) { seg[di] = nums[ni++]; }
          if (ctx.setLineDash) { ctx.setLineDash(seg); }
          break;
        }
        case SURF.CREATE_LINEAR_GRADIENT:
          curGradient = ctx.createLinearGradient(nums[ni++], nums[ni++], nums[ni++], nums[ni++]);
          break;
        case SURF.CREATE_RADIAL_GRADIENT:
          curGradient = ctx.createRadialGradient(nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++]);
          break;
        case SURF.ADD_COLOR_STOP: {
          var off = nums[ni++], col = objs[oi++];
          if (curGradient) { curGradient.addColorStop(off, col); }
          break;
        }
        case SURF.SET_FILL_GRADIENT: if (curGradient) { ctx.fillStyle = curGradient; } break;
        case SURF.SET_STROKE_GRADIENT: if (curGradient) { ctx.strokeStyle = curGradient; } break;
        case SURF.CREATE_PATTERN: {
          var pimg = surfaceImageSource(objs[oi++]), prep = objs[oi++];
          try { curPattern = ctx.createPattern(pimg, prep); } catch (_ep) { curPattern = null; }
          break;
        }
        case SURF.CREATE_PATTERN_SURFACE: {
          var psurf = surfaceTable[nums[ni++]], prep2 = objs[oi++];
          try { curPattern = (psurf && psurf.canvas) ? ctx.createPattern(psurf.canvas, prep2) : null; } catch (_eps) { curPattern = null; }
          break;
        }
        case SURF.SET_FILL_PATTERN: if (curPattern) { ctx.fillStyle = curPattern; } break;
        case SURF.DRAW_IMAGE_XY: {
          var i1 = surfaceImageSource(objs[oi++]);
          if (i1) { ctx.drawImage(i1, nums[ni++], nums[ni++]); } else { ni += 2; }
          break;
        }
        case SURF.DRAW_IMAGE_XYWH: {
          var i2 = surfaceImageSource(objs[oi++]);
          if (i2) { ctx.drawImage(i2, nums[ni++], nums[ni++], nums[ni++], nums[ni++]); } else { ni += 4; }
          break;
        }
        case SURF.DRAW_IMAGE_SRCDST: {
          var i3 = surfaceImageSource(objs[oi++]);
          if (i3) { ctx.drawImage(i3, nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++]); } else { ni += 8; }
          break;
        }
        case SURF.BLIT_SURFACE_XY: {
          var b1 = surfaceTable[nums[ni++]];
          if (b1 && b1.canvas) { ctx.drawImage(b1.canvas, nums[ni++], nums[ni++]); } else { ni += 2; }
          break;
        }
        case SURF.BLIT_SURFACE_XYWH: {
          var b2 = surfaceTable[nums[ni++]];
          if (b2 && b2.canvas) { ctx.drawImage(b2.canvas, nums[ni++], nums[ni++], nums[ni++], nums[ni++]); } else { ni += 4; }
          break;
        }
        case SURF.BLIT_SURFACE_SRCDST: {
          var b3 = surfaceTable[nums[ni++]];
          if (b3 && b3.canvas) { ctx.drawImage(b3.canvas, nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++], nums[ni++]); } else { ni += 8; }
          break;
        }
        default:
          if (global.console && global.console.warn) {
            global.console.warn('surface replay: unknown opcode ' + code);
          }
          break;
      }
    }
  }

  // Create / resize a surface. Fire-and-forget. Idempotent: an existing surface
  // of the same id is resized (which also clears it, matching a fresh image).
  hostBridge.register('__cn1_surface_create__', function(request) {
    var r = request || {};
    var id = r.id | 0;
    var w = r.w | 0;
    var h = r.h | 0;
    var s = surfaceTable[id];
    if (!s) {
      getSurface(id, w, h);
    } else if (s.canvas && (s.canvas.width !== w || s.canvas.height !== h)) {
      s.canvas.width = w;
      s.canvas.height = h;
    }
    return null;
  });

  // Replay a command batch onto a surface. Fire-and-forget.
  hostBridge.register('__cn1_surface_flush__', function(request) {
    var r = request || {};
    var id = r.id | 0;
    var s = getSurface(id, r.w | 0, r.h | 0);
    if (!s || !s.ctx) {
      return null;
    }
    var ops = r.ops || [];
    var opCount = r.opCount | 0;
    if (id === SURF_DISPLAY_ID) {
      // Give the DISPLAY context a clean baseline before replaying this frame.
      // A draw op (e.g. a decorated drawString or a gradient) can record an
      // unbalanced save/clip/transform; the recorded frame's single save/restore
      // then pops the wrong level and leaks a clip that INTERSECTS into every
      // later frame -- the drawable region shrinks to nothing and the display
      // freezes (observed: the whole graphics/chart/theme block captured a stale
      // frame after DrawStringDecorated). Popping any outstanding saves and
      // forcing identity here defeats that leak; the recorded ops re-establish
      // the crop. restore() on an empty stack is a no-op, and this resets context
      // STATE only -- pixels are untouched so partial-frame updates still
      // composite correctly.
      for (var __ri = 0; __ri < 64; __ri++) {
        s.ctx.restore();
      }
      s.ctx.setTransform(1, 0, 0, 1, 0, 0);
    }
    replaySurfaceCommands(s.ctx, ops, opCount, r.nums || [], r.objs || []);
    // Surface flushes replay straight onto the canvas context, bypassing the
    // jso-bridge ``noteDrawTarget`` path that tracks per-canvas paintCount /
    // lastPaintSeq. The screenshot capture + UI-settle heuristics rely on those
    // counters to know the display painted; without this nudge they wait forever
    // for a paint that "never happened" (the display surface renders but reads as
    // paintCount=0). Mark the display canvas painted and advance the render-queue
    // sequence so canvas-pick / settle see the frame.
    if (id === SURF_DISPLAY_ID && opCount > 0) {
      noteCanvasOperation(s.canvas, 'method', 'fill', true, null);
      global.__cn1RenderQueueSeq = (global.__cn1RenderQueueSeq | 0) + 1;
    }
    return null;
  });

  // Read back pixels as ARGB ints (getRGB). The ONE surface op that returns
  // data. ``dest`` is filled in place to avoid re-allocating a large array.
  hostBridge.register('__cn1_surface_read__', function(request) {
    var r = request || {};
    var s = surfaceTable[r.id | 0];
    if (!s || !s.ctx) {
      return null;
    }
    var x = r.x | 0, y = r.y | 0, w = r.w | 0, h = r.h | 0;
    var img;
    try {
      img = s.ctx.getImageData(x, y, w, h);
    } catch (_er) {
      return null;
    }
    var data = img.data;
    var n = w * h;
    var out = new Array(n);
    for (var i = 0; i < n; i++) {
      var p = i << 2;
      out[i] = ((data[p + 3] & 0xff) << 24)
        | ((data[p] & 0xff) << 16)
        | ((data[p + 1] & 0xff) << 8)
        | (data[p + 2] & 0xff);
    }
    return out;
  });

  // Encode a surface's backing canvas to a base64 data URL (PNG/JPEG export +
  // the animation-grid screenshot path). The image bytes read-back path.
  hostBridge.register('__cn1_surface_to_dataurl__', function(request) {
    var r = request || {};
    var s = surfaceTable[r.id | 0];
    if (!s || !s.canvas || typeof s.canvas.toDataURL !== 'function') {
      return null;
    }
    try {
      var q = (typeof r.quality === 'number' && r.quality >= 0 && r.quality <= 1) ? r.quality : undefined;
      var url = s.canvas.toDataURL(r.mime || 'image/png', q);
      if (!url || url.length < 32) {
        return null;
      }
      return url;
    } catch (e) {
      return null;
    }
  });

  // Release a surface's backing canvas when its owning Java image is GC'd.
  hostBridge.register('__cn1_surface_dispose__', function(request) {
    var r = request || {};
    var id = r.id | 0;
    if (id !== SURF_DISPLAY_ID) {
      delete surfaceTable[id];
    }
    return null;
  });

  // Write a raw ARGB pixel rectangle onto a surface (createImage(int[])).
  hostBridge.register('__cn1_surface_write__', function(request) {
    var r = request || {};
    var w = r.w | 0, h = r.h | 0;
    var s = getSurface(r.id | 0, w, h);
    if (!s || !s.ctx || w <= 0 || h <= 0) {
      return null;
    }
    var argb = r.argb || [];
    var img = s.ctx.createImageData(w, h);
    var data = img.data;
    var n = w * h;
    for (var i = 0; i < n; i++) {
      var v = argb[i] | 0;
      var p = i << 2;
      data[p] = (v >>> 16) & 0xff;
      data[p + 1] = (v >>> 8) & 0xff;
      data[p + 2] = v & 0xff;
      data[p + 3] = (v >>> 24) & 0xff;
    }
    s.ctx.putImageData(img, 0, 0);
    return null;
  });

  // Read back an ARGB pixel rectangle from a LOADED image host resource. The
  // image is drawn onto a scratch canvas (the worker holds no canvas) and its
  // pixels read + packed ARGB. The one read-back for loaded-image getRGB.
  hostBridge.register('__cn1_image_read__', function(request) {
    var r = request || {};
    var image = resolveHostRef(r.image);
    var w = r.w | 0, h = r.h | 0;
    if (!image || w <= 0 || h <= 0) {
      return null;
    }
    var doc = global.document || (global.window && global.window.document);
    if (!doc || !doc.createElement) {
      return null;
    }
    var cv = doc.createElement('canvas');
    cv.width = w;
    cv.height = h;
    var ctx = cv.getContext('2d');
    var data;
    try {
      ctx.drawImage(image, r.x | 0, r.y | 0, w, h, 0, 0, w, h);
      data = ctx.getImageData(0, 0, w, h).data;
    } catch (_eir) {
      return null;
    }
    var n = w * h;
    var out = new Array(n);
    for (var i = 0; i < n; i++) {
      var p = i << 2;
      out[i] = ((data[p + 3] & 0xff) << 24)
        | ((data[p] & 0xff) << 16)
        | ((data[p + 1] & 0xff) << 8)
        | (data[p + 2] & 0xff);
    }
    return out;
  });

  // Gaussian blur from a loaded image (srcSurfaceId < 0) or another surface,
  // onto a destination surface, in one canvas2d filter:blur op.
  hostBridge.register('__cn1_surface_blur__', function(request) {
    var r = request || {};
    var w = r.w | 0, h = r.h | 0;
    var dst = getSurface(r.dstId | 0, w, h);
    if (!dst || !dst.ctx) {
      return null;
    }
    var src;
    if ((r.srcSurfaceId | 0) >= 0) {
      var ss = surfaceTable[r.srcSurfaceId | 0];
      src = ss && ss.canvas;
    } else {
      src = resolveHostRef(r.srcImage);
    }
    if (!src) {
      return null;
    }
    try {
      dst.ctx.save();
      dst.ctx.clearRect(0, 0, w, h);
      dst.ctx.filter = 'blur(' + (+r.radius) + 'px)';
      dst.ctx.drawImage(src, 0, 0, w, h);
      dst.ctx.restore();
    } catch (_eb) {}
    return null;
  });

  // Append a surface's backing canvas into a DOM element and style it (native
  // widgets that embed a CN1-rendered image directly in the page).
  hostBridge.register('__cn1_attach_surface_to_element__', function(request) {
    var r = request || {};
    var s = surfaceTable[r.id | 0];
    var el = resolveHostRef(r.element);
    if (!s || !s.canvas || !el) {
      return null;
    }
    if (r.cssWidth != null && s.canvas.style) {
      s.canvas.style.width = r.cssWidth;
    }
    if (r.cssHeight != null && s.canvas.style) {
      s.canvas.style.height = r.cssHeight;
    }
    try { el.appendChild(s.canvas); } catch (_ea) {}
    return null;
  });

  // Hide the splash element on the main thread. The translated
  // ``HTML5Implementation.hideSplash`` body uses ``jQuery(...)``
  // directly, but the worker context has no jQuery (and no DOM).
  // The corresponding worker-side ``bindCiFallback`` in port.js
  // detects the missing jQuery and routes to this host handler so
  // the actual splash removal happens on the main thread where
  // jQuery / the DOM are available. Falls back to a manual remove
  // when jQuery isn't loaded on the main thread either (e.g. when
  // the bundle is served standalone without the website wrapper).
  hostBridge.register('__cn1_hide_splash__', function() {
    var doc = (global.window || global).document || global.document;
    if (!doc) {
      return null;
    }
    var splash = doc.getElementById('cn1-splash');
    if (!splash) {
      return null;
    }
    var jq = (global.window || global).jQuery || global.jQuery;
    if (typeof jq === 'function') {
      try {
        jq(splash).fadeOut(100, function() { jq(this).remove(); });
        return null;
      } catch (_e) {
        // Fall through to manual remove on jQuery error.
      }
    }
    if (splash.parentNode) {
      splash.parentNode.removeChild(splash);
    }
    return null;
  });

  // Save-blob handler (used by HTML5Implementation.execute for downloads).
  // The worker can't touch ``document`` so all the link-creation +
  // .click() driving the actual file save has to live on the main thread.
  // We stash the pending handler here and the cn1NativeBacksideHooks
  // user-gesture poll fires ``__cn1_fire_save_blob__`` which invokes it
  // -- preserving the "download must be inside a user-gesture event"
  // browser contract.
  var __cn1PendingSaveBlobHandler = null;

  function __cn1MakeBlobDownloader(blob, fileName) {
    return function() {
      var doc = (global.window || global).document || global.document;
      var win = global.window || global;
      if (!doc) {
        return;
      }
      if (win.navigator && win.navigator.msSaveOrOpenBlob) {
        win.navigator.msSaveOrOpenBlob(blob, fileName);
        return;
      }
      var a = doc.createElement('a');
      a.href = (win.URL || URL).createObjectURL(blob);
      a.download = fileName;
      doc.body.appendChild(a);
      a.click();
      doc.body.removeChild(a);
    };
  }

  hostBridge.register('__cn1_register_save_blob__', function(request) {
    var payload = request || {};
    var blob = resolveHostRef(payload.blob);
    var fileName = String(payload.fileName == null ? 'download' : payload.fileName);
    if (global.console && typeof global.console.log === 'function') {
      try { global.console.log('CN1INIT:save-blob:register fileName=' + fileName + ' blob=' + (blob ? 'ok' : 'missing')); } catch (_le) {}
    }
    if (!blob) {
      __cn1PendingSaveBlobHandler = null;
      return null;
    }
    // Fire the download immediately. The Generate flow is a clear user-
    // intent path so most browsers allow the programmatic ``a.click()``
    // even though the original ``mousedown`` was ~10s ago (cooperative
    // scheduler had to walk the zip template). Backside-hook timing
    // becomes irrelevant.
    var handler = __cn1MakeBlobDownloader(blob, fileName);
    try { handler(); } catch (e) {
      if (global.console && typeof global.console.warn === 'function') {
        try { global.console.warn('PARPAR:save-blob-immediate-failed:' + (e && e.message ? e.message : String(e))); } catch (_le) {}
      }
    }
    // Also stash for the existing backside-hook fire path in case the
    // immediate click was blocked by user-gesture policy.
    __cn1PendingSaveBlobHandler = __cn1MakeBlobDownloader(blob, fileName);
    return null;
  });

  hostBridge.register('__cn1_register_save_blob_dataurl__', function(request) {
    var payload = request || {};
    var dataUrl = String(payload.dataUrl == null ? '' : payload.dataUrl);
    var fileName = String(payload.fileName == null ? 'download' : payload.fileName);
    if (!dataUrl) {
      __cn1PendingSaveBlobHandler = null;
      return null;
    }
    __cn1PendingSaveBlobHandler = function() {
      var doc = (global.window || global).document || global.document;
      if (!doc) {
        return;
      }
      var a = doc.createElement('a');
      a.href = dataUrl;
      a.download = fileName;
      doc.body.appendChild(a);
      a.click();
      doc.body.removeChild(a);
    };
    return null;
  });

  hostBridge.register('__cn1_deregister_save_blob__', function() {
    __cn1PendingSaveBlobHandler = null;
    return null;
  });

  hostBridge.register('__cn1_fire_save_blob__', function() {
    var handler = __cn1PendingSaveBlobHandler;
    __cn1PendingSaveBlobHandler = null;
    if (global.console && typeof global.console.log === 'function') {
      try { global.console.log('CN1INIT:save-blob:fire handler=' + (typeof handler === 'function' ? 'present' : 'absent')); } catch (_le) {}
    }
    if (typeof handler === 'function') {
      try { handler(); } catch (e) {
        if (global.console && typeof global.console.warn === 'function') {
          try { global.console.warn('PARPAR:save-blob-failed:' + (e && e.message ? e.message : String(e))); } catch (_le) {}
        }
      }
    }
    return null;
  });

  // Apply a CSS canvas2d ``filter: blur(<radius>px)`` to ``dst`` from
  // ``src`` in a single host-side call. The worker invokes this once per
  // ``gaussianBlurImage`` so the cooperative scheduler doesn't have to
  // round-trip six separate ctx.save/setFilter/drawImage/setFilter/restore
  // ops -- the earlier per-op Java-typed dispatch path was correct but
  // slow enough to hang Sheet/Dialog backdrop renders against the screenshot
  // test budget (see fa18f0301..153d971dc). Returns null; callers don't
  // need the result, just the side effect on ``dst``.
  hostBridge.register('__cn1_apply_canvas_blur__', function(request) {
    var payload = request || {};
    var dst = resolveHostRef(payload.dst);
    var src = resolveHostRef(payload.src);
    if (!dst || !src || typeof dst.getContext !== 'function') {
      return null;
    }
    var w = (payload.w | 0);
    var h = (payload.h | 0);
    if (w <= 0 || h <= 0) {
      return null;
    }
    var radius = +payload.radius;
    if (!(radius >= 0)) {
      radius = 0;
    }
    var ctx = dst.getContext('2d');
    if (!ctx) {
      return null;
    }
    ctx.save();
    try {
      ctx.filter = 'blur(' + radius + 'px)';
      ctx.drawImage(src, 0, 0, w, h);
    } finally {
      ctx.filter = 'none';
      ctx.restore();
    }
    return null;
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
        // Race rAF against a setTimeout fallback. Headless Chromium throttles
        // and ultimately STOPS firing requestAnimationFrame when the page idles
        // (no compositing) -- which happens right after a form transition
        // completes and the worker's EDT parks on __cn1_wait_for_ui_settle__,
        // leaving nothing to drive a frame. Without the fallback this runFrame
        // chain never resolves, the host never replies, and the EDT parks
        // forever (the SlideHorizontalTransitionTest wall). The fallback
        // guarantees forward progress; when rAF is healthy it wins the race so
        // steady-state timing is unchanged.
        var advanced = false;
        function tick() {
          if (advanced) {
            return;
          }
          advanced = true;
          remaining--;
          if (remaining <= 0) {
            resolve();
            return;
          }
          step();
        }
        raf(tick);
        setTimeout(tick, 32);
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
    // Cache the score per canvas, keyed on its last draw-op sequence. The
    // scoring below does 9 getImageData() GPU readbacks; pickBestCanvasSnapshot
    // runs it over EVERY tracked canvas, and the suite leaks hundreds of
    // off-screen mutable-image canvases (FinalizationRegistry release never
    // fires under back-to-back load), so a late capture would otherwise pay
    // ~700x9 readbacks -- slow captures that pressure the worker<->host channel
    // into the lost-response wedge. A canvas not drawn since its last score
    // (stable lastSeq) returns the cached value; the display canvas is painted
    // every frame so its lastSeq advances and it is always freshly scored.
    var meta = getCanvasMeta(canvas);
    if (meta && meta.__cn1ScoreSeq === meta.lastSeq && '__cn1ScoreVal' in meta) {
      return meta.__cn1ScoreVal;
    }
    var result = canvasContentScoreCompute(canvas, w, h);
    if (meta) {
      meta.__cn1ScoreSeq = meta.lastSeq;
      meta.__cn1ScoreVal = result;
    }
    return result;
  }
  function canvasContentScoreCompute(canvas, w, h) {
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
    if (data.type === 'releaseHostRef') {
      releaseHostRefs(data.ids);
      return;
    }
    if (data.type === 'host-call') {
      hostBridge.invoke(data.symbol, data.args || [], target || global.__parparWorker, data.id);
      return;
    }
    if (data.type === 'host-call-batch') {
      // Batched fire-and-forget JSO bridge ops. The worker emits these
      // at end-of-drain to amortise structured-clone postMessage cost
      // across all canvas/DOM setters or void method calls in a paint
      // burst. Each op carries its own ``__cn1_no_response`` flag, so
      // hostBridge.invoke skips the postHostCallback path naturally.
      var ops = data.ops || [];
      for (var oi = 0; oi < ops.length; oi++) {
        try {
          hostBridge.invoke('__cn1_jso_bridge__', [ops[oi]], target || global.__parparWorker, 0);
        } catch (e) {
          if (global.console && typeof global.console.error === 'function') {
            global.console.error('host-call-batch op[' + oi + '] failed: ' + (e && e.message || e));
          }
        }
      }
      return;
    }
    if (data.type === 'result') {
      global.__parparResult = data;
      global.cn1Started = true;
      return;
    }
    if (data.type === 'lifecycle' && data.phase === 'started') {
      // Worker emits this once when the main bytecode generator
      // completes — Lifecycle.init and Lifecycle.start both
      // returned. The pre-existing fallbacks (CN1JS:.runApp log
      // probe + ``type: result`` System.exit hook) only fire for
      // the screenshot test fixtures (which run an explicit suite)
      // and the unit-test System.exit pattern. A regular app that
      // reaches its first form and waits for input never produced
      // either signal — manifested as ``cn1Started`` staying false
      // forever in the lifecycle test harness.
      global.cn1Started = true;
      return;
    }
    if (data.type === 'error') {
      global.__parparError = data;
      // ALWAYS surface runtime errors to the main-thread console — this is
      // unrelated to the diagEnabled diagnostics toggle. Without this, an
      // app crash inside the worker vanishes silently because diag() is
      // gated, and users only see the "Loading..." splash hang forever.
      if (global.console && typeof global.console.error === 'function') {
        var errorText = 'PARPAR:ERROR: ' + (data.message || 'unknown');
        if (data.stack) {
          errorText += '\n' + data.stack;
        }
        if (data.virtualFailure) {
          try {
            errorText += '\n  virtualFailure=' + JSON.stringify(data.virtualFailure);
          } catch (_jse) {
            errorText += '\n  virtualFailure=[unserialisable]';
          }
        }
        global.console.error(errorText);
      }
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
      // Forwarded log messages from the worker. We still have to inspect
      // the message body below (CN1SS:INFO:suite starting drives the
      // screenshot harness state, and CN1JS:RenderQueue.* updates the
      // paint-seq counter) so the *detection* path is unconditional; we
      // only suppress the main-thread console echo unless diagnostics
      // are enabled. That echo was the source of the doubled
      // PARPAR:DIAG:* lines in the production browser console.
      if (diagEnabled && global.console && typeof global.console.log === 'function') {
        global.console.log(String(data.message));
      } else if (global.console && typeof global.console.log === 'function') {
        // Allowlist a small set of high-value diagnostic prefixes so they
        // surface on the main-thread console even without ?parparDiag=1.
        // System.out.println from user / framework code uses these tags
        // exactly because the diag-gated echo above swallows everything;
        // app-level breadcrumbs and the raw-JS-error catch warning need
        // to be visible in production deployments to be useful.
        var rawMsg = String(data.message);
        if (rawMsg.indexOf('CN1INIT:') === 0
                || rawMsg.indexOf('PARPAR:CAUGHT_RAW_JS_ERROR') === 0
                || rawMsg.indexOf('PARPAR:ERROR') === 0
                || rawMsg.indexOf('CN1SS:ERR:') === 0) {
          global.console.log(rawMsg);
        }
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
