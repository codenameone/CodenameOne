/**
 * JavaScriptPort - Native bindings for JSO interfaces
 * This file contains native implementations for the com.codename1.html5.js.* interfaces
 * used by the JavaScript port of Codename One.
 */

(function(global) {
  const jsoRegistry = global.jvm && global.jvm.jsoRegistry;
  if (!jsoRegistry) {
    console.warn("JSO registry not found in VM runtime. JSO bindings may not work correctly.");
    return;
  }

  jsoRegistry.classPrefixes.push(
    "com_codename1_html5_js_",
    "com_codename1_impl_html5_JSOImplementations_"
  );

  jsoRegistry.inferFn = function(value, expectedClass, jvm) {
    if (value === (global.window || global.self || global)) {
      return "com_codename1_html5_js_browser_Window";
    }
    if (value && value.nodeType === 9) {
      return "com_codename1_html5_js_dom_HTMLDocument";
    }
    if (typeof global.ArrayBuffer !== "undefined" && value instanceof global.ArrayBuffer) {
      return "com_codename1_html5_js_typedarrays_ArrayBuffer";
    }
    if (typeof global.Uint8ClampedArray !== "undefined" && value instanceof global.Uint8ClampedArray) {
      return "com_codename1_html5_js_typedarrays_Uint8ClampedArray";
    }
    if (typeof global.Uint8Array !== "undefined" && value instanceof global.Uint8Array) {
      return "com_codename1_html5_js_typedarrays_Uint8Array";
    }
    if (value && value.canvas && typeof value.drawImage === "function" && typeof value.fillRect === "function") {
      return "com_codename1_html5_js_canvas_CanvasRenderingContext2D";
    }
    if (value && value.data && value.width !== undefined && value.height !== undefined && typeof value.data.length === "number") {
      return "com_codename1_html5_js_canvas_ImageData";
    }
    if (value && value.setProperty && value.removeProperty) {
      return "com_codename1_html5_js_dom_CSSStyleDeclaration";
    }
    if (value && value.href != null && value.assign && value.replace) {
      if (jvm.classes["com_codename1_impl_html5_JSOImplementations_WindowLocation"]) {
        return "com_codename1_impl_html5_JSOImplementations_WindowLocation";
      }
      return expectedClass || "com_codename1_html5_js_browser_Location";
    }
    if (value && value.tagName) {
      const tagName = String(value.tagName).toUpperCase();
      switch (tagName) {
        case "CANVAS":
          return "com_codename1_html5_js_dom_HTMLCanvasElement";
        case "IMG":
          return "com_codename1_html5_js_dom_HTMLImageElement";
        case "INPUT":
          return "com_codename1_html5_js_dom_HTMLInputElement";
        case "TEXTAREA":
          return "com_codename1_html5_js_dom_HTMLTextAreaElement";
        case "BODY":
          return "com_codename1_html5_js_dom_HTMLBodyElement";
        case "IFRAME":
          return jvm.classes["com_codename1_impl_html5_JSOImplementations_HTMLIFrameElement"] 
            ? "com_codename1_impl_html5_JSOImplementations_HTMLIFrameElement" 
            : "com_codename1_html5_js_dom_HTMLElement";
        default:
          return expectedClass || "com_codename1_html5_js_dom_HTMLElement";
      }
    }
    if (value && value.type !== undefined && value.target !== undefined) {
      return expectedClass || "com_codename1_html5_js_dom_Event";
    }
    return null;
  };

  jsoRegistry.nativeArgConverters.push(function(value, jvm) {
    if (jvm.instanceOf(value, "com_codename1_html5_js_dom_EventListener")) {
      if (value.__nativeEventListener) {
        return value.__nativeEventListener;
      }
      value.__nativeEventListener = function(event) {
        try {
          const wrappedEvent = jvm.wrapJsResult(event, "com_codename1_html5_js_dom_Event");
          const method = jvm.resolveVirtual(value.__class, "cn1_com_codename1_html5_js_dom_EventListener_handleEvent_com_codename1_html5_js_dom_Event");
          jvm.spawn(null, method(value, wrappedEvent));
        } catch (err) {
          jvm.fail(err);
        }
      };
      return value.__nativeEventListener;
    }
    if (jvm.instanceOf(value, "com_codename1_html5_js_browser_AnimationFrameCallback")) {
      if (value.__nativeAnimationFrameCallback) {
        return value.__nativeAnimationFrameCallback;
      }
      value.__nativeAnimationFrameCallback = function(time) {
        try {
          spawnVirtualCallback(
            value,
            "cn1_com_codename1_html5_js_browser_AnimationFrameCallback_onAnimationFrame_double",
            [+time],
            "__cn1RafCallbackPending"
          );
        } catch (err) {
          jvm.fail(err);
        }
      };
      return value.__nativeAnimationFrameCallback;
    }
    return value;
  });
})(self);

function isPhoneUserAgent() {
  const agent = (global.navigator && global.navigator.userAgent) || "";
  if (!agent) {
    return false;
  }
  const mobileRegex = /(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows ce|xda|xiino/i;
  const prefixRegex = /1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i;
  return mobileRegex.test(agent) || prefixRegex.test(agent.substring(0, 4));
}

function isPhoneOrTabletUserAgent() {
  const agent = (global.navigator && global.navigator.userAgent) || "";
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
  return /Mac/.test((global.navigator && global.navigator.userAgent) || "");
}

function isIPadUserAgent() {
  const nav = global.navigator || {};
  const ua = String(nav.userAgent || "");
  const platform = String(nav.platform || "");
  const maxTouchPoints = Number(nav.maxTouchPoints || 0);
  return /iPad/i.test(ua) || (platform === "MacIntel" && maxTouchPoints > 1);
}

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

const ciFallbackMarkerSeen = Object.create(null);
function emitCiFallbackMarker(symbol, markerType) {
  const key = markerType + ":" + symbol;
  if (ciFallbackMarkerSeen[key]) {
    return;
  }
  ciFallbackMarkerSeen[key] = true;
  if (global.console && typeof global.console.log === "function") {
    global.console.log("PARPAR:DIAG:FALLBACK:key=FALLBACK:" + symbol + ":" + markerType);
  }
}
function bindCiFallback(symbol, names, fn) {
  emitCiFallbackMarker(symbol, "ENABLED");
  const wrappedFallback = function*() {
    emitCiFallbackMarker(symbol, "HIT");
    return yield* fn.apply(this, arguments);
  };
  wrappedFallback.__cn1CiFallbackSymbol = symbol;
  bindNative(names, wrappedFallback);
}
function bindCiFallbackWithMethodId(symbol, names, fn) {
  emitCiFallbackMarker(symbol, "ENABLED");
  for (let i = 0; i < names.length; i++) {
    const methodId = names[i];
    const wrappedFallback = function*() {
      emitCiFallbackMarker(symbol, "HIT");
      const args = new Array(arguments.length + 1);
      args[0] = methodId;
      for (let j = 0; j < arguments.length; j++) {
        args[j + 1] = arguments[j];
      }
      return yield* fn.apply(this, args);
    };
    wrappedFallback.__cn1CiFallbackSymbol = symbol;
    bindNative([methodId], wrappedFallback);
  }
}
function aliasGlobalToImpl(symbol) {
  if (typeof global[symbol] === "function") {
    return false;
  }
  const impl = global[symbol + "__impl"];
  if (typeof impl !== "function") {
    return false;
  }
  global[symbol] = impl;
  emitDiagLine("PARPAR:DIAG:INIT:aliasGlobalToImpl=" + symbol);
  return true;
}

function spawnVirtualCallback(receiver, methodId, args, pendingFlagKey) {
  if (!receiver || !receiver.__class) {
    return false;
  }
  if (pendingFlagKey && receiver[pendingFlagKey]) {
    return false;
  }
  if (pendingFlagKey) {
    receiver[pendingFlagKey] = true;
  }
  let method = null;
  try {
    method = jvm.resolveVirtual(receiver.__class, methodId);
  } catch (err) {
    if (pendingFlagKey) {
      receiver[pendingFlagKey] = false;
    }
    throw err;
  }
  function* run() {
    try {
      return yield* method.apply(null, [receiver].concat(args || []));
    } finally {
      if (pendingFlagKey) {
        receiver[pendingFlagKey] = false;
      }
    }
  }
  jvm.spawn(null, run());
  return true;
}

function* stringifyThrowable(throwable) {
  if (!throwable || !throwable.__class) {
    if (throwable == null) {
      return "null";
    }
    return String(throwable);
  }
  const className = String(throwable.__class || "java_lang_Throwable");
  const pieces = [className];
  if (throwable.message) {
    pieces.push("jsMessage=" + String(throwable.message));
  }
  if (throwable.cn1_java_lang_Throwable_detailMessage && throwable.cn1_java_lang_Throwable_detailMessage.__class === "java_lang_String") {
    try {
      pieces.push("detail=" + jvm.toNativeString(throwable.cn1_java_lang_Throwable_detailMessage));
    } catch (_err) {
      // Best effort diagnostic path only.
    }
  }
  try {
    const toStringMethod = jvm.resolveVirtual(throwable.__class, "cn1_java_lang_Throwable_toString_R_java_lang_String");
    const value = yield* toStringMethod(throwable);
    if (value && value.__class === "java_lang_String") {
      pieces.push(jvm.toNativeString(value));
    }
  } catch (_err) {
    // Best effort diagnostic path only.
  }
  try {
    const messageMethod = jvm.resolveVirtual(throwable.__class, "cn1_java_lang_Throwable_getMessage_R_java_lang_String");
    const message = yield* messageMethod(throwable);
    if (message && message.__class === "java_lang_String") {
      pieces.push("message=" + jvm.toNativeString(message));
    }
  } catch (_err) {
    // Best effort diagnostic path only.
  }
  try {
    const printStackTraceMethod = jvm.resolveVirtual(throwable.__class, "cn1_java_lang_Throwable_printStackTrace");
    yield* printStackTraceMethod(throwable);
    pieces.push("stack=printed");
  } catch (_err) {
    // Best effort diagnostic path only.
  }
  const cause = throwable.cn1_java_lang_Throwable_cause;
  if (cause && cause.__class) {
    pieces.push("cause=" + String(cause.__class));
  }
  try {
    const keys = Object.keys(throwable);
    if (keys.length > 0) {
      pieces.push("keys=" + keys.slice(0, 8).join(","));
    }
  } catch (_err) {
    // Best effort diagnostic path only.
  }
  return pieces.join(" | ");
}

function checkDisplayInitState() {
  const displayClass = jvm && jvm.classes ? jvm.classes["com_codename1_ui_Display"] : null;
  if (!displayClass) {
    return { displayClassExists: false, instance: null, edt: null };
  }
  const instanceField = displayClass.staticFields ? displayClass.staticFields["INSTANCE"] : null;
  const edtValue = instanceField && instanceField.cn1_java_lang_Display_edt ? instanceField.cn1_java_lang_Display_edt : null;
  return {
    displayClassExists: true,
    instance: instanceField,
    edt: edtValue,
    edtThreadName: edtValue && edtValue.cn1_java_lang_Thread_name ? function() {
      try {
        const nameField = edtValue.cn1_java_lang_Thread_name;
        return nameField && nameField.__class === "java_lang_String" ? jvm.toNativeString(nameField) : String(nameField);
      } catch (_e) {
        return String(edtValue.cn1_java_lang_Thread_name);
      }
    }() : null
  };
}

function ensureDisplayEdt() {
  const state = checkDisplayInitState();
  if (state.edt) {
    return true;
  }
  if (!state.instance) {
    emitDiagLine("PARPAR:DIAG:EDT_ENSURE:instanceMissing=1");
    return false;
  }
  const threadClass = jvm.classes && jvm.classes["java_lang_Thread"];
  if (!threadClass) {
    emitDiagLine("PARPAR:DIAG:EDT_ENSURE:threadClassMissing=1");
    return false;
  }
  const mainThread = jvm.mainThreadObject || (jvm.currentThread && jvm.currentThread.object);
  if (!mainThread) {
    emitDiagLine("PARPAR:DIAG:EDT_ENSURE:mainThreadMissing=1");
    return false;
  }
  const existingActive = threadClass.staticFields && threadClass.staticFields["activeThreads"];
  if (existingActive && existingActive > 0) {
    state.instance.cn1_java_lang_Display_edt = mainThread;
    emitDiagLine("PARPAR:DIAG:EDT_ENSURE:reusedMainThread=1");
    return true;
  }
  const edtThread = jvm.newObject("java_lang_Thread");
  edtThread.cn1_java_lang_Thread_alive = 1;
  edtThread.cn1_java_lang_Thread_name = jvm.createStringLiteral("EDT");
  edtThread.cn1_java_lang_Thread_nativeThreadId = jvm.nextThreadId++;
  state.instance.cn1_java_lang_Display_edt = edtThread;
  if (threadClass.staticFields) {
    threadClass.staticFields["activeThreads"] = (threadClass.staticFields["activeThreads"] || 0) + 1;
  }
  emitDiagLine("PARPAR:DIAG:EDT_ENSURE:createdSyntheticEdt=1");
  return true;
}

function emitDisplayInitDiag(marker) {
  const state = checkDisplayInitState();
  emitDiagLine("PARPAR:DIAG:" + marker + ":displayClassExists=" + (state.displayClassExists ? "1" : "0")+ ":instance=" + (state.instance ? "present" : "null")+ ":edt=" + (state.edt ? "present" : "null") + (state.edtThreadName ? ":edtThreadName=" + state.edtThreadName : ""));
}

// Enable forwarding System.out.println output to the main thread via postMessage.
// This is only needed in the browser JS port where Playwright cannot reliably
// capture Worker console.log.  Detect the browser Worker context by checking
// for the native importScripts function (not the polyfill used in Node.js
// worker_threads test harnesses which uses vm.runInThisContext).
global.__cn1ForwardConsoleToMain = (typeof WorkerGlobalScope !== "undefined"
    || (typeof self !== "undefined" && typeof self.importScripts === "function" && typeof process === "undefined"));

function emitDiagLine(line) {
  if (global.console && typeof global.console.log === "function") {
    global.console.log(line);
  }
  // Forward to main thread so Playwright (page.on('console')) can capture
  // CN1SS output from the worker.  Worker console.log is not always
  // observable from the page context.
  if (typeof global.postMessage === "function") {
    try {
      global.postMessage({ type: "log", message: String(line) });
    } catch (postErr) {
      if (global.console && typeof global.console.warn === "function") {
        global.console.warn("emitDiagLine:postMessage failed: " + String(postErr && postErr.message ? postErr.message : postErr));
      }
    }
  }
}

function wrapVirtualMethodWithDiag(className, methodId, marker) {
  if (!jvm || !jvm.classes || !jvm.classes[className]) {
    return false;
  }
  const classDef = jvm.classes[className];
  if (!classDef.methods || typeof classDef.methods[methodId] !== "function") {
    return false;
  }
  const original = classDef.methods[methodId];
  if (original.__cn1DiagWrapped) {
    return true;
  }
  const wrapped = function*() {
    emitDiagLine("PARPAR:DIAG:" + marker + ":enter");
    try {
      const result = yield* original.apply(this, arguments);
      emitDiagLine("PARPAR:DIAG:" + marker + ":exit");
      return result;
    } catch (err) {
      const detail = yield* stringifyThrowable(err);
      emitDiagLine("PARPAR:DIAG:" + marker + ":error=" + detail);
      throw err;
    }
  };
  wrapped.__cn1DiagWrapped = true;
  classDef.methods[methodId] = wrapped;
  return true;
}

function wrapGlobalGeneratorWithDiag(symbol, marker) {
  if (typeof global[symbol] !== "function") {
    return false;
  }
  const original = global[symbol];
  if (original.__cn1DiagWrapped) {
    return true;
  }
  const wrapped = function*() {
    emitDiagLine("PARPAR:DIAG:" + marker + ":enter");
    try {
      const result = yield* original.apply(this, arguments);
      emitDiagLine("PARPAR:DIAG:" + marker + ":exit");
      return result;
    } catch (err) {
      const detail = yield* stringifyThrowable(err);
      emitDiagLine("PARPAR:DIAG:" + marker + ":error=" + detail);
      throw err;
    }
  };
  wrapped.__cn1DiagWrapped = true;
  global[symbol] = wrapped;
  return true;
}

function installLifecycleDiagnostics() {
  if (!getQueryParameter("parparDiag")) {
    return;
  }
  const targets = [
    ["com_codename1_ui_Form", "cn1_com_codename1_ui_Form_show", "FORM_SHOW"],
    ["com_codename1_ui_Form", "cn1_com_codename1_ui_Form_onShowCompletedImpl", "FORM_ON_SHOW_COMPLETED_IMPL"],
    ["com_codename1_ui_Form", "cn1_com_codename1_ui_Form_onShowCompleted", "FORM_ON_SHOW_COMPLETED"],
    ["com_codename1_ui_Display", "cn1_com_codename1_ui_Display_setCurrentForm_com_codename1_ui_Form", "DISPLAY_SET_CURRENT_FORM"],
    ["com_codename1_system_Lifecycle", "cn1_com_codename1_system_Lifecycle_setCurrentForm_com_codename1_ui_Form", "LIFECYCLE_SET_CURRENT_FORM"],
    ["com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner", "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_runSuite", "CN1SS_RUNNER_SUITE"],
    ["com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner", "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_runNextTest_int", "CN1SS_RUNNER_NEXT_INDEX"],
    ["com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner", "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_lambda_runNextTest_2_java_lang_String_com_codenameone_examples_hellocodenameone_tests_BaseTest_int", "CN1SS_RUNNER_NEXT_TEST"],
    ["com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner", "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_finalizeTest_int_com_codenameone_examples_hellocodenameone_tests_BaseTest_java_lang_String_boolean", "CN1SS_RUNNER_FINALIZE_TEST"],
    ["com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner", "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_finishSuite", "CN1SS_RUNNER_FINISH_SUITE"],
    ["com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner", "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_log_java_lang_String", "CN1SS_RUNNER_LOG"]
  ];
  let wrappedCount = 0;
  for (let i = 0; i < targets.length; i++) {
    const target = targets[i];
    const ok = wrapVirtualMethodWithDiag(target[0], target[1], target[2]);
    if (ok) {
      wrappedCount++;
    }
    emitDiagLine("PARPAR:DIAG:INIT:lifecycleDiagWrap:" + target[2] + "=" + (ok ? "1" : "0"));
  }
  emitDiagLine("PARPAR:DIAG:INIT:lifecycleDiagWrapped=" + wrappedCount);

  const globalTargets = [
    ["cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_createForm_java_lang_String_com_codename1_ui_layouts_Layout_java_lang_String_R_com_codename1_ui_Form", "BASETEST_CREATE_FORM"],
    ["cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_1___INIT___com_codenameone_examples_hellocodenameone_tests_BaseTest_java_lang_String_com_codename1_ui_layouts_Layout_java_lang_String", "BASETEST_FORM_INIT"],
    ["cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_1_onShowCompleted", "BASETEST_ONSHOW_COMPLETED"],
    ["cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_1_onShowCompleted__impl", "BASETEST_ONSHOW_COMPLETED_IMPL"],
    ["cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_1_lambda_onShowCompleted_0_java_lang_String", "BASETEST_ONSHOW_LAMBDA"],
    ["cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_1_lambda_onShowCompleted_0_java_lang_String__impl", "BASETEST_ONSHOW_LAMBDA_IMPL"],
    ["cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunnerHelper_emitCurrentFormScreenshot_java_lang_String_java_lang_Runnable", "CN1SS_HELPER_EMIT_SCREENSHOT"],
    ["cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunnerHelper_emitCurrentFormScreenshot_java_lang_String_java_lang_Runnable__impl", "CN1SS_HELPER_EMIT_SCREENSHOT_IMPL"],
    ["cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunnerHelper_lambda_emitCurrentFormScreenshot_0_java_lang_String_java_lang_Runnable_int_int_com_codename1_ui_Image", "CN1SS_HELPER_EMIT_SCREENSHOT_LAMBDA"],
    ["cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunnerHelper_lambda_emitCurrentFormScreenshot_0_java_lang_String_java_lang_Runnable_int_int_com_codename1_ui_Image__impl", "CN1SS_HELPER_EMIT_SCREENSHOT_LAMBDA_IMPL"],
    ["cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunnerHelper_emitChannel_byte_1ARRAY_java_lang_String_java_lang_String", "CN1SS_HELPER_EMIT_CHANNEL"],
    ["cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunnerHelper_emitChannel_byte_1ARRAY_java_lang_String_java_lang_String__impl", "CN1SS_HELPER_EMIT_CHANNEL_IMPL"],
    ["cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunnerHelper_complete_java_lang_Runnable", "CN1SS_HELPER_COMPLETE"],
    ["cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunnerHelper_complete_java_lang_Runnable__impl", "CN1SS_HELPER_COMPLETE_IMPL"],
    ["cn1_com_codename1_ui_Form___INIT___java_lang_String_com_codename1_ui_layouts_Layout", "FORM_INIT_LAYOUT"],
    ["cn1_com_codename1_ui_Form_onShowCompletedImpl", "FORM_ON_SHOW_COMPLETED_IMPL_GLOBAL"],
    ["cn1_com_codename1_ui_Display_setCurrent_com_codename1_ui_Form_boolean", "DISPLAY_SET_CURRENT"]
  ];
  for (let i = 0; i < globalTargets.length; i++) {
    const target = globalTargets[i];
    const ok = wrapGlobalGeneratorWithDiag(target[0], target[1]);
    emitDiagLine("PARPAR:DIAG:INIT:globalDiagWrap:" + target[1] + "=" + (ok ? "1" : "0"));
  }
}

installLifecycleDiagnostics();
function ensureKotlinUnitShim() {
  if (!jvm || typeof jvm.defineClass !== "function" || !jvm.classes) {
    return;
  }
  if (jvm.classes["kotlin_Unit"]) {
    return;
  }
  jvm.defineClass({
    name: "kotlin_Unit",
    baseClass: "java_lang_Object",
    interfaces: [],
    isInterface: false,
    isAbstract: false,
    assignableTo: { "kotlin_Unit": true, "java_lang_Object": true },
    instanceFields: [],
    staticFields: { "INSTANCE": null },
    methods: {},
    classObject: null
  });
  function* cn1_kotlin_Unit___INIT__(__cn1ThisObject) {
    yield* cn1_java_lang_Object___INIT__(__cn1ThisObject);
    return null;
  }
  function* cn1_kotlin_Unit_toString_R_java_lang_String() {
    return jvm.createStringLiteral("kotlin.Unit");
  }
  jvm.addVirtualMethod("kotlin_Unit", "cn1_kotlin_Unit_toString_R_java_lang_String", cn1_kotlin_Unit_toString_R_java_lang_String);
  jvm.classes["kotlin_Unit"].clinit = function*() {
    const unit = jvm.newObject("kotlin_Unit");
    yield* cn1_kotlin_Unit___INIT__(unit);
    jvm.classes["kotlin_Unit"].staticFields["INSTANCE"] = unit;
    return null;
  };
  emitDiagLine("PARPAR:DIAG:INIT:shim=kotlinUnit");
}
ensureKotlinUnitShim();

function installMissingGlobalDelegate(symbol, delegateSymbol, marker) {
  if (typeof global[symbol] === "function") {
    return false;
  }
  global[symbol] = function*() {
    emitCiFallbackMarker(marker, "HIT");
    const delegate = global[delegateSymbol];
    if (typeof delegate === "function") {
      return yield* delegate.apply(this, arguments);
    }
    return null;
  };
  emitCiFallbackMarker(marker, "ENABLED");
  emitDiagLine("PARPAR:DIAG:INIT:missingGlobalDelegate:" + symbol + "->" + delegateSymbol);
  return true;
}

function installMissingOwnerDelegates(owner, delegateOwner, suffixes, markerPrefix) {
  for (let i = 0; i < suffixes.length; i++) {
    const suffix = suffixes[i];
    installMissingGlobalDelegate(
      "cn1_" + owner + "_" + suffix,
      "cn1_" + delegateOwner + "_" + suffix,
      markerPrefix + "." + suffix
    );
  }
}

function installInferredMissingOwnerDelegates(owner, delegateOwner, markerPrefix) {
  const ownerPrefix = "cn1_" + owner + "_";
  const delegatePrefix = "cn1_" + delegateOwner + "_";
  const usagePattern = new RegExp(ownerPrefix + "([A-Za-z0-9_]+)", "g");
  const suffixes = Object.create(null);
  const keys = Object.keys(global);
  for (let i = 0; i < keys.length; i++) {
    const key = keys[i];
    if (typeof global[key] !== "function" || key.indexOf("cn1_") !== 0) {
      continue;
    }
    let source = "";
    try {
      source = Function.prototype.toString.call(global[key]);
    } catch (_err) {
      source = "";
    }
    if (!source || source.indexOf(ownerPrefix) < 0) {
      continue;
    }
    usagePattern.lastIndex = 0;
    let match;
    while ((match = usagePattern.exec(source)) !== null) {
      if (match[1]) {
        suffixes[match[1]] = true;
      }
    }
  }
  const names = Object.keys(suffixes);
  let installed = 0;
  for (let i = 0; i < names.length; i++) {
    const suffix = names[i];
    const symbol = ownerPrefix + suffix;
    const delegate = delegatePrefix + suffix;
    if (typeof global[symbol] === "function" || typeof global[delegate] !== "function") {
      continue;
    }
    if (installMissingGlobalDelegate(symbol, delegate, markerPrefix + "." + suffix)) {
      installed++;
    }
  }
  emitDiagLine("PARPAR:DIAG:INIT:inferredMissingOwnerDelegates:" + owner + "->" + delegateOwner + ":installed=" + installed);
  return installed;
}

installMissingGlobalDelegate(
  "cn1_com_codename1_ui_Label_focusGainedInternal",
  "cn1_com_codename1_ui_Component_focusGainedInternal",
  "Label.focusGainedInternalMissing"
);
installMissingGlobalDelegate(
  "cn1_com_codename1_ui_Label_focusLostInternal",
  "cn1_com_codename1_ui_Component_focusLostInternal",
  "Label.focusLostInternalMissing"
);
installMissingGlobalDelegate(
  "cn1_com_codename1_ui_Label_getStyle_R_com_codename1_ui_plaf_Style",
  "cn1_com_codename1_ui_Component_getStyle_R_com_codename1_ui_plaf_Style",
  "Label.getStyleMissing"
);
installMissingGlobalDelegate(
  "cn1_com_codename1_ui_Label_getUIManager_R_com_codename1_ui_plaf_UIManager",
  "cn1_com_codename1_ui_Component_getUIManager_R_com_codename1_ui_plaf_UIManager",
  "Label.getUIManagerMissing"
);
installMissingGlobalDelegate(
  "cn1_com_codename1_ui_Label_keyPressed_int",
  "cn1_com_codename1_ui_Component_keyPressed_int",
  "Label.keyPressedMissing"
);
installMissingGlobalDelegate(
  "cn1_com_codename1_ui_Label_keyReleased_int",
  "cn1_com_codename1_ui_Component_keyReleased_int",
  "Label.keyReleasedMissing"
);
installMissingGlobalDelegate(
  "cn1_com_codename1_ui_Label_paintComponentBackground_com_codename1_ui_Graphics",
  "cn1_com_codename1_ui_Component_paintComponentBackground_com_codename1_ui_Graphics",
  "Label.paintComponentBackgroundMissing"
);
installMissingGlobalDelegate(
  "cn1_com_codename1_ui_Label_fireActionEvent",
  "cn1_com_codename1_ui_Component_fireActionEvent",
  "Label.fireActionEventMissing"
);
installMissingGlobalDelegate(
  "cn1_com_codename1_ui_Button_initLaf_com_codename1_ui_plaf_UIManager",
  "cn1_com_codename1_ui_Component_initLaf_com_codename1_ui_plaf_UIManager",
  "Button.initLafMissing"
);
installMissingOwnerDelegates(
  "com_codename1_ui_Container",
  "com_codename1_ui_Component",
  [
    "animate_R_boolean",
    "deinitialize",
    "fireActionEvent",
    "getComponentForm_R_com_codename1_ui_Form",
    "getPreferredW_R_int",
    "getPropertyValue_java_lang_String_R_java_lang_Object",
    "initComponent",
    "initUnselectedStyle_com_codename1_ui_plaf_Style",
    "internalPaintImpl_com_codename1_ui_Graphics_boolean",
    "isVisible_R_boolean",
    "paintBackground_com_codename1_ui_Graphics",
    "paintScrollbars_com_codename1_ui_Graphics",
    "putClientProperty_java_lang_String_java_lang_Object",
    "repaint_com_codename1_ui_Component",
    "setHeight_int",
    "setPropertyValue_java_lang_String_java_lang_Object_R_java_lang_String",
    "setRTL_boolean",
    "setScrollY_int",
    "setUIID_java_lang_String",
    "setUnselectedStyle_com_codename1_ui_plaf_Style",
    "setWidth_int",
    "setX_int",
    "setY_int",
    "styleChanged_java_lang_String_com_codename1_ui_plaf_Style"
  ],
  "Container.missing"
);
installInferredMissingOwnerDelegates("com_codename1_ui_Label", "com_codename1_ui_Component", "Label.inferred");
installInferredMissingOwnerDelegates("com_codename1_ui_Container", "com_codename1_ui_Component", "Container.inferred");
installInferredMissingOwnerDelegates("com_codename1_ui_TextArea", "com_codename1_ui_Component", "TextArea.inferred");
installInferredMissingOwnerDelegates("com_codename1_ui_TextField", "com_codename1_ui_TextArea", "TextField.inferred");
installInferredMissingOwnerDelegates("com_codename1_ui_Form", "com_codename1_ui_Container", "Form.inferred");
installInferredMissingOwnerDelegates("com_codename1_ui_List", "com_codename1_ui_Container", "List.inferred");
installInferredMissingOwnerDelegates("com_codename1_ui_PeerComponent", "com_codename1_ui_Component", "PeerComponent.inferred");

if (typeof global.cn1_com_codename1_ui_Container_setVisible_boolean !== "function") {
  global.cn1_com_codename1_ui_Container_setVisible_boolean = function*(__cn1ThisObject, visible) {
    if (!__cn1ThisObject) {
      return null;
    }
    const containerClass = jvm.classes && jvm.classes["com_codename1_ui_Container"];
    const containerMethod = containerClass && containerClass.methods
      ? containerClass.methods["cn1_com_codename1_ui_Container_setVisible_boolean"]
      : null;
    if (typeof containerMethod === "function") {
      return yield* containerMethod(__cn1ThisObject, visible);
    }
    const componentClass = jvm.classes && jvm.classes["com_codename1_ui_Component"];
    const componentMethod = componentClass && componentClass.methods
      ? componentClass.methods["cn1_com_codename1_ui_Component_setVisible_boolean"]
      : null;
    if (typeof componentMethod === "function") {
      return yield* componentMethod(__cn1ThisObject, visible);
    }
    return null;
  };
  emitDiagLine("PARPAR:DIAG:INIT:shim=containerSetVisibleDirect");
}
if (typeof global.cn1_com_codename1_ui_Container_setAlwaysTensile_boolean !== "function") {
  global.cn1_com_codename1_ui_Container_setAlwaysTensile_boolean = function*(__cn1ThisObject, enabled) {
    if (!__cn1ThisObject) {
      return null;
    }
    const containerClass = jvm.classes && jvm.classes["com_codename1_ui_Container"];
    const containerMethod = containerClass && containerClass.methods
      ? containerClass.methods["cn1_com_codename1_ui_Container_setAlwaysTensile_boolean"]
      : null;
    if (typeof containerMethod === "function") {
      return yield* containerMethod(__cn1ThisObject, enabled);
    }
    const componentClass = jvm.classes && jvm.classes["com_codename1_ui_Component"];
    const componentMethod = componentClass && componentClass.methods
      ? componentClass.methods["cn1_com_codename1_ui_Component_setAlwaysTensile_boolean"]
      : null;
    if (typeof componentMethod === "function") {
      return yield* componentMethod(__cn1ThisObject, enabled);
    }
    return null;
  };
  emitDiagLine("PARPAR:DIAG:INIT:shim=containerSetAlwaysTensileDirect");
}
if (typeof global.cn1_com_codename1_ui_PeerComponent_styleChanged_java_lang_String_com_codename1_ui_plaf_Style !== "function") {
  global.cn1_com_codename1_ui_PeerComponent_styleChanged_java_lang_String_com_codename1_ui_plaf_Style = function*(__cn1ThisObject, propertyName, style) {
    if (!__cn1ThisObject) {
      return null;
    }
    const componentStyleChanged = global.cn1_com_codename1_ui_Component_styleChanged_java_lang_String_com_codename1_ui_plaf_Style;
    if (typeof componentStyleChanged === "function") {
      return yield* componentStyleChanged(__cn1ThisObject, propertyName, style);
    }
    return null;
  };
  emitDiagLine(
    "PARPAR:DIAG:INIT:missingGlobalDelegate:cn1_com_codename1_ui_PeerComponent_styleChanged_java_lang_String_com_codename1_ui_plaf_Style"
    + "->cn1_com_codename1_ui_Component_styleChanged_java_lang_String_com_codename1_ui_plaf_Style"
  );
}

bindNative(["cn1_com_codename1_html5_js_core_JSArray_create_R_com_codename1_html5_js_core_JSArray", "cn1_com_codename1_html5_js_core_JSArray_create___R_com_codename1_html5_js_core_JSArray"], function*() {
  const arr = [];
  return jvm.wrapJsObject(arr, "com_codename1_html5_js_core_JSArray");
});

bindNative(["cn1_com_codename1_html5_js_core_JSArray_create_int_R_com_codename1_html5_js_core_JSArray", "cn1_com_codename1_html5_js_core_JSArray_create___int_R_com_codename1_html5_js_core_JSArray"], function*(length) {
  const size = Math.max(0, length | 0);
  const arr = new Array(size);
  for (let i = 0; i < size; i++) {
    arr[i] = null;
  }
  return jvm.wrapJsObject(arr, "com_codename1_html5_js_core_JSArray");
});

bindNative(["cn1_com_codename1_html5_js_browser_Window_current_R_com_codename1_html5_js_browser_Window", "cn1_com_codename1_html5_js_browser_Window_current___R_com_codename1_html5_js_browser_Window"], function*() {
  const nativeWindow = global.window;
  const hasDomWindow = !!(nativeWindow && nativeWindow.document);
  if (!hasDomWindow && typeof jvm.invokeHostNative === "function") {
    const hostWindow = yield jvm.invokeHostNative("__cn1_dom_window_current__", []);
    if (hostWindow != null) {
      const workerWrapper = jvm.wrapJsObject(hostWindow, "com_codename1_html5_js_browser_Window");
      jvm.enhanceJsWrapper(workerWrapper, "com_codename1_impl_html5_JSOImplementations_WindowExt");
      return workerWrapper;
    }
  }
  const wrapper = jvm.wrapJsObject((hasDomWindow ? nativeWindow : null) || global.self || global, "com_codename1_html5_js_browser_Window");
  jvm.enhanceJsWrapper(wrapper, "com_codename1_impl_html5_JSOImplementations_WindowExt");
  return wrapper;
});

bindNative([
  "cn1_com_codename1_html5_js_browser_Window_getDocument_R_com_codename1_html5_js_dom_HTMLDocument",
  "cn1_com_codename1_html5_js_browser_Window_getDocument___R_com_codename1_html5_js_dom_HTMLDocument"
], function*(__cn1ThisObject) {
  const documentExtClass = "com_codename1_impl_html5_JSOImplementations_DocumentExt";
  const win = jvm.unwrapJsValue(__cn1ThisObject);
  if (win && win.__cn1HostRef != null && typeof jvm.invokeHostNative === "function") {
    const hostResult = yield jvm.invokeHostNative("__cn1_jso_bridge__", [{
      receiver: win,
      kind: "getter",
      member: "document",
      args: []
    }]);
    if (hostResult == null) {
      return null;
    }
    const docWrapper = jvm.wrapJsObject(hostResult, "com_codename1_html5_js_dom_HTMLDocument");
    jvm.enhanceJsWrapper(docWrapper, documentExtClass);
    return docWrapper;
  }
  if (typeof jvm.invokeHostNative === "function" && (!win || !win.document)) {
    const hostWindow = yield jvm.invokeHostNative("__cn1_dom_window_current__", []);
    if (hostWindow != null) {
      const hostDocument = yield jvm.invokeHostNative("__cn1_jso_bridge__", [{
        receiver: hostWindow,
        kind: "getter",
        member: "document",
        args: []
      }]);
      if (hostDocument == null) {
        return null;
      }
      const docWrapper = jvm.wrapJsObject(hostDocument, "com_codename1_html5_js_dom_HTMLDocument");
      jvm.enhanceJsWrapper(docWrapper, documentExtClass);
      return docWrapper;
    }
  }
  if (!win || !win.document) {
    return null;
  }
  const docWrapper = jvm.wrapJsObject(win.document, "com_codename1_html5_js_dom_HTMLDocument");
  jvm.enhanceJsWrapper(docWrapper, documentExtClass);
  return docWrapper;
});

bindNative([
  "cn1_com_codename1_html5_js_dom_HTMLDocument_createElement_java_lang_String_R_com_codename1_html5_js_dom_HTMLElement",
  "cn1_com_codename1_html5_js_dom_HTMLDocument_createElement___java_lang_String_R_com_codename1_html5_js_dom_HTMLElement"
], function*(__cn1ThisObject, tagName) {
  const doc = jvm.unwrapJsValue(__cn1ThisObject);
  const tag = tagName == null ? "" : jvm.toNativeString(tagName);
  const canvasClass = "com_codename1_html5_js_dom_HTMLCanvasElement";
  if (doc && doc.__cn1HostRef != null && typeof jvm.invokeHostNative === "function") {
    const hostResult = yield jvm.invokeHostNative("__cn1_jso_bridge__", [{
      receiver: doc,
      kind: "method",
      member: "createElement",
      args: [tag]
    }]);
    if (hostResult == null) {
      return null;
    }
    const expectedClass = String(tag).toLowerCase() === "canvas"
      ? canvasClass
      : jvm.inferJsObjectClass(hostResult, "com_codename1_html5_js_dom_HTMLElement");
    return jvm.wrapJsObject(hostResult, expectedClass);
  }
  if (!doc || typeof doc.createElement !== "function") {
    return null;
  }
  const element = doc.createElement(tag);
  const expectedClass = String(tag).toLowerCase() === "canvas"
    ? canvasClass
    : jvm.inferJsObjectClass(element, "com_codename1_html5_js_dom_HTMLElement");
  return jvm.wrapJsObject(element, expectedClass);
});

bindNative([
  "cn1_com_codename1_html5_js_dom_HTMLDocument_getBody_R_com_codename1_html5_js_dom_HTMLElement",
  "cn1_com_codename1_html5_js_dom_HTMLDocument_getBody___R_com_codename1_html5_js_dom_HTMLElement"
], function*(__cn1ThisObject) {
  const doc = jvm.unwrapJsValue(__cn1ThisObject);
  if (doc && doc.__cn1HostRef != null && typeof jvm.invokeHostNative === "function") {
    const hostResult = yield jvm.invokeHostNative("__cn1_jso_bridge__", [{
      receiver: doc,
      kind: "getter",
      member: "body",
      args: []
    }]);
    return hostResult == null ? null : jvm.wrapJsObject(hostResult, "com_codename1_html5_js_dom_HTMLBodyElement");
  }
  if (!doc || !doc.body) {
    return null;
  }
  return jvm.wrapJsObject(doc.body, "com_codename1_html5_js_dom_HTMLBodyElement");
});

bindNative([
  "cn1_com_codename1_html5_js_dom_HTMLDocument_getElementById_java_lang_String_R_com_codename1_html5_js_dom_HTMLElement",
  "cn1_com_codename1_html5_js_dom_HTMLDocument_getElementById___java_lang_String_R_com_codename1_html5_js_dom_HTMLElement"
], function*(__cn1ThisObject, id) {
  const doc = jvm.unwrapJsValue(__cn1ThisObject);
  const nativeId = id == null ? "" : jvm.toNativeString(id);
  const canvasClass = "com_codename1_html5_js_dom_HTMLCanvasElement";
  if (doc && doc.__cn1HostRef != null && typeof jvm.invokeHostNative === "function") {
    const hostResult = yield jvm.invokeHostNative("__cn1_jso_bridge__", [{
      receiver: doc,
      kind: "method",
      member: "getElementById",
      args: [nativeId]
    }]);
    if (hostResult == null) {
      return null;
    }
    const tagName = yield jvm.invokeHostNative("__cn1_jso_bridge__", [{
      receiver: hostResult,
      kind: "getter",
      member: "tagName",
      args: []
    }]);
    const expectedClass = String(tagName || "").toUpperCase() === "CANVAS"
      ? canvasClass
      : jvm.inferJsObjectClass(hostResult, "com_codename1_html5_js_dom_HTMLElement");
    return jvm.wrapJsObject(hostResult, expectedClass);
  }
  if (!doc || typeof doc.getElementById !== "function") {
    return null;
  }
  const element = doc.getElementById(nativeId);
  return element == null ? null : jvm.wrapJsObject(element, jvm.inferJsObjectClass(element, "com_codename1_html5_js_dom_HTMLElement"));
});

bindNative([
  "cn1_com_codename1_html5_js_ajax_XMLHttpRequest_create_R_com_codename1_html5_js_ajax_XMLHttpRequest",
  "cn1_com_codename1_html5_js_ajax_XMLHttpRequest_create___R_com_codename1_html5_js_ajax_XMLHttpRequest"
], function*() {
  if (typeof global.XMLHttpRequest !== "function") {
    throw new Error("XMLHttpRequest is not available in this javascript runtime");
  }
  return jvm.wrapJsObject(new global.XMLHttpRequest(), "com_codename1_html5_js_ajax_XMLHttpRequest");
});

bindNative([
  "cn1_com_codename1_html5_js_typedarrays_ArrayBuffer_create_int_R_com_codename1_html5_js_typedarrays_ArrayBuffer",
  "cn1_com_codename1_html5_js_typedarrays_ArrayBuffer_create___int_R_com_codename1_html5_js_typedarrays_ArrayBuffer"
], function*(size) {
  return jvm.wrapJsObject(new global.ArrayBuffer(size | 0), "com_codename1_html5_js_typedarrays_ArrayBuffer");
});

bindNative([
  "cn1_com_codename1_html5_js_typedarrays_Uint8Array_create_int_R_com_codename1_html5_js_typedarrays_Uint8Array",
  "cn1_com_codename1_html5_js_typedarrays_Uint8Array_create___int_R_com_codename1_html5_js_typedarrays_Uint8Array"
], function*(size) {
  return jvm.wrapJsObject(new global.Uint8Array(size | 0), "com_codename1_html5_js_typedarrays_Uint8Array");
});

// Float64Array factory methods — needed by HTML5Implementation.transformPoint /
// transformPoints and anywhere scene-graph / spinner rendering converts float[] to
// a typed array for native matrix math. Without these the `static create(int)` in
// Float64Array.java returns its stub `null`, crashing the worker with
// `TypeError: Cannot read properties of null (reading '__classDef')`.
bindNative([
  "cn1_com_codename1_html5_js_typedarrays_Float64Array_create_int_R_com_codename1_html5_js_typedarrays_Float64Array",
  "cn1_com_codename1_html5_js_typedarrays_Float64Array_create___int_R_com_codename1_html5_js_typedarrays_Float64Array"
], function*(size) {
  return jvm.wrapJsObject(new global.Float64Array(size | 0), "com_codename1_html5_js_typedarrays_Float64Array");
});

bindNative([
  "cn1_com_codename1_html5_js_typedarrays_Float64Array_create_com_codename1_html5_js_typedarrays_ArrayBuffer_R_com_codename1_html5_js_typedarrays_Float64Array",
  "cn1_com_codename1_html5_js_typedarrays_Float64Array_create___com_codename1_html5_js_typedarrays_ArrayBuffer_R_com_codename1_html5_js_typedarrays_Float64Array"
], function*(buffer) {
  return jvm.wrapJsObject(new global.Float64Array(jvm.unwrapJsValue(buffer)), "com_codename1_html5_js_typedarrays_Float64Array");
});

bindNative([
  "cn1_com_codename1_html5_js_typedarrays_Uint8Array_create_com_codename1_html5_js_typedarrays_ArrayBuffer_R_com_codename1_html5_js_typedarrays_Uint8Array",
  "cn1_com_codename1_html5_js_typedarrays_Uint8Array_create___com_codename1_html5_js_typedarrays_ArrayBuffer_R_com_codename1_html5_js_typedarrays_Uint8Array"
], function*(buffer) {
  return jvm.wrapJsObject(new global.Uint8Array(jvm.unwrapJsValue(buffer)), "com_codename1_html5_js_typedarrays_Uint8Array");
});

bindNative([
  "cn1_com_codename1_html5_js_typedarrays_Uint8Array_create_com_codename1_html5_js_typedarrays_ArrayBufferView_R_com_codename1_html5_js_typedarrays_Uint8Array",
  "cn1_com_codename1_html5_js_typedarrays_Uint8Array_create___com_codename1_html5_js_typedarrays_ArrayBufferView_R_com_codename1_html5_js_typedarrays_Uint8Array"
], function*(bufferView) {
  const nativeView = jvm.unwrapJsValue(bufferView);
  return jvm.wrapJsObject(new global.Uint8Array(nativeView.buffer, nativeView.byteOffset || 0, nativeView.byteLength || undefined), "com_codename1_html5_js_typedarrays_Uint8Array");
});

bindNative([
  "cn1_com_codename1_html5_js_typedarrays_Uint8Array_create_com_codename1_html5_js_typedarrays_ArrayBuffer_int_R_com_codename1_html5_js_typedarrays_Uint8Array",
  "cn1_com_codename1_html5_js_typedarrays_Uint8Array_create___com_codename1_html5_js_typedarrays_ArrayBuffer_int_R_com_codename1_html5_js_typedarrays_Uint8Array"
], function*(buffer, offset) {
  return jvm.wrapJsObject(new global.Uint8Array(jvm.unwrapJsValue(buffer), offset | 0), "com_codename1_html5_js_typedarrays_Uint8Array");
});

bindNative([
  "cn1_com_codename1_html5_js_typedarrays_Uint8Array_create_com_codename1_html5_js_typedarrays_ArrayBuffer_int_int_R_com_codename1_html5_js_typedarrays_Uint8Array",
  "cn1_com_codename1_html5_js_typedarrays_Uint8Array_create___com_codename1_html5_js_typedarrays_ArrayBuffer_int_int_R_com_codename1_html5_js_typedarrays_Uint8Array"
], function*(buffer, offset, length) {
  return jvm.wrapJsObject(new global.Uint8Array(jvm.unwrapJsValue(buffer), offset | 0, length | 0), "com_codename1_html5_js_typedarrays_Uint8Array");
});

// Uint8ClampedArray factory methods – needed by createImageData() in
// HTML5Implementation which converts ARGB int[] pixels into canvas ImageData.
bindNative([
  "cn1_com_codename1_html5_js_typedarrays_Uint8ClampedArray_create_int_R_com_codename1_html5_js_typedarrays_Uint8ClampedArray",
  "cn1_com_codename1_html5_js_typedarrays_Uint8ClampedArray_create___int_R_com_codename1_html5_js_typedarrays_Uint8ClampedArray"
], function*(size) {
  return jvm.wrapJsObject(new global.Uint8ClampedArray(size | 0), "com_codename1_html5_js_typedarrays_Uint8ClampedArray");
});

bindNative([
  "cn1_com_codename1_html5_js_typedarrays_Uint8ClampedArray_create_com_codename1_html5_js_typedarrays_ArrayBuffer_R_com_codename1_html5_js_typedarrays_Uint8ClampedArray",
  "cn1_com_codename1_html5_js_typedarrays_Uint8ClampedArray_create___com_codename1_html5_js_typedarrays_ArrayBuffer_R_com_codename1_html5_js_typedarrays_Uint8ClampedArray"
], function*(buffer) {
  return jvm.wrapJsObject(new global.Uint8ClampedArray(jvm.unwrapJsValue(buffer)), "com_codename1_html5_js_typedarrays_Uint8ClampedArray");
});

bindNative([
  "cn1_com_codename1_html5_js_typedarrays_Uint8ClampedArray_create_com_codename1_html5_js_typedarrays_ArrayBuffer_int_R_com_codename1_html5_js_typedarrays_Uint8ClampedArray",
  "cn1_com_codename1_html5_js_typedarrays_Uint8ClampedArray_create___com_codename1_html5_js_typedarrays_ArrayBuffer_int_R_com_codename1_html5_js_typedarrays_Uint8ClampedArray"
], function*(buffer, offset) {
  return jvm.wrapJsObject(new global.Uint8ClampedArray(jvm.unwrapJsValue(buffer), offset | 0), "com_codename1_html5_js_typedarrays_Uint8ClampedArray");
});

bindNative([
  "cn1_com_codename1_html5_js_typedarrays_Uint8ClampedArray_create_com_codename1_html5_js_typedarrays_ArrayBuffer_int_int_R_com_codename1_html5_js_typedarrays_Uint8ClampedArray",
  "cn1_com_codename1_html5_js_typedarrays_Uint8ClampedArray_create___com_codename1_html5_js_typedarrays_ArrayBuffer_int_int_R_com_codename1_html5_js_typedarrays_Uint8ClampedArray"
], function*(buffer, offset, length) {
  return jvm.wrapJsObject(new global.Uint8ClampedArray(jvm.unwrapJsValue(buffer), offset | 0, length | 0), "com_codename1_html5_js_typedarrays_Uint8ClampedArray");
});

bindNative([
  "cn1_com_codename1_impl_html5_HTML5Implementation_createCNOutboxEvent_java_lang_String_int_R_com_codename1_html5_js_dom_Event",
  "cn1_com_codename1_impl_html5_HTML5Implementation_createCNOutboxEvent___java_lang_String_int_R_com_codename1_html5_js_dom_Event"
], function*(message, code) {
  const win = global.window || global.self || global;
  const detail = message == null ? null : jvm.toNativeString(message);
  if (typeof jvm.invokeHostNative === "function" && (!win || !win.document)) {
    const hostEvent = yield jvm.invokeHostNative("__cn1_create_custom_event__", [{
      type: "cn1outbox",
      detail: detail,
      code: code | 0
    }]);
    return hostEvent == null ? null : jvm.wrapJsObject(hostEvent, "com_codename1_html5_js_dom_Event");
  }
  const event = new win.CustomEvent("cn1outbox", { detail: detail, code: code |0 });
  return jvm.wrapJsObject(event, "com_codename1_html5_js_dom_Event");
});

bindNative([
  "cn1_com_codename1_impl_html5_HTML5Implementation_createCustomEvent_java_lang_String_java_lang_String_int_R_com_codename1_html5_js_dom_Event",
  "cn1_com_codename1_impl_html5_HTML5Implementation_createCustomEvent___java_lang_String_java_lang_String_int_R_com_codename1_html5_js_dom_Event"
], function*(type, message, code) {
  const win = global.window || global.self || global;
  const eventType = type == null ? "" : jvm.toNativeString(type);
  const detail = message == null ? null : jvm.toNativeString(message);
  if (typeof jvm.invokeHostNative === "function" && (!win || !win.document)) {
    const hostEvent = yield jvm.invokeHostNative("__cn1_create_custom_event__", [{
      type: eventType,
      detail: detail,
      code: code | 0
    }]);
    return hostEvent == null ? null : jvm.wrapJsObject(hostEvent, "com_codename1_html5_js_dom_Event");
  }
  const event = new win.CustomEvent(eventType, { detail: detail, code: code |0 });
  return jvm.wrapJsObject(event, "com_codename1_html5_js_dom_Event");
});

bindNative(["cn1_com_codename1_impl_html5_HTML5Implementation_getParameterByName_java_lang_String_R_java_lang_String", "cn1_com_codename1_impl_html5_HTML5Implementation_getParameterByName___java_lang_String_R_java_lang_String"], function*(name) {
  const value = getQueryParameter(jvm.toNativeString(name));
  return value == null ? null : jvm.createStringLiteral(value);
});

bindNative(["cn1_com_codename1_impl_html5_HTML5Implementation_getDevicePixelRatio__R_double", "cn1_com_codename1_impl_html5_HTML5Implementation_getDevicePixelRatio___R_double"], function*() {
  const ratioOverride = getQueryParameter("pixelRatio");
  const win = global.window || global;
  if (ratioOverride != null && ratioOverride !== "") {
    const parsed = Number(ratioOverride);
    if (!isNaN(parsed) && parsed > 0) {
      win.overridePixelRatio = parsed;
    } else {
      win.overridePixelRatio = 0;
    }
  } else if (typeof win.overridePixelRatio === "undefined") {
    win.overridePixelRatio = 0;
  }
  if (typeof win.cn1ScaleCoord === "undefined") {
    win.cn1ScaleCoord = function(x) {
      return x === -1 ? -1 : x / (win.overridePixelRatio || win.devicePixelRatio || 1.0);
    };
  }
  if (typeof win.cn1UnscaleCoord === "undefined") {
    win.cn1UnscaleCoord = function(x) {
      return x === -1 ? -1 : x * (win.overridePixelRatio || win.devicePixelRatio || 1.0);
    };
  }
  return Number(win.overridePixelRatio || win.devicePixelRatio || 1.0);
});

bindNative(["cn1_com_codename1_impl_html5_HTML5Implementation_getBaseFontSize_R_int", "cn1_com_codename1_impl_html5_HTML5Implementation_getBaseFontSize___R_int"], function*() {
  const value = getQueryParameter("baseFont");
  if (value == null || value === "") {
    return 0;
  }
  const parsed = parseInt(value, 10);
  return isNaN(parsed) ? 0 : parsed |0;
});

bindNative(["cn1_com_codename1_impl_html5_HTML5Implementation_getDensityOverride_R_int", "cn1_com_codename1_impl_html5_HTML5Implementation_getDensityOverride___R_int"], function*() {
  const value = getQueryParameter("density");
  if (value == null || value === "") {
    return 0;
  }
  const parsed = parseInt(value, 10);
  return isNaN(parsed) ? 0 : parsed |0;
});

bindNative(["cn1_com_codename1_impl_html5_HTML5Implementation_isPhone__R_boolean", "cn1_com_codename1_impl_html5_HTML5Implementation_isPhone___R_boolean"], function*() {
  return isPhoneUserAgent() ? 1 : 0;
});

bindNative(["cn1_com_codename1_impl_html5_HTML5Implementation_isPhoneOrTablet__R_boolean", "cn1_com_codename1_impl_html5_HTML5Implementation_isPhoneOrTablet___R_boolean"], function*() {
  return isPhoneOrTabletUserAgent() ? 1 : 0;
});

bindNative(["cn1_com_codename1_impl_html5_HTML5Implementation_isIOS_R_boolean", "cn1_com_codename1_impl_html5_HTML5Implementation_isIOS___R_boolean"], function*() {
  return isIOSUserAgent() ? 1 : 0;
});

bindNative(["cn1_com_codename1_impl_html5_HTML5Implementation_isMac_R_boolean", "cn1_com_codename1_impl_html5_HTML5Implementation_isMac___R_boolean"], function*() {
  return isMacUserAgent() ? 1 : 0;
});

bindNative(["cn1_com_codename1_impl_html5_HTML5Implementation_isIPad_R_boolean", "cn1_com_codename1_impl_html5_HTML5Implementation_isIPad___R_boolean"], function*() {
  return isIPadUserAgent() ? 1 : 0;
});

bindNative(["cn1_com_codename1_impl_html5_HTML5Implementation_getBrowserLanguage_R_java_lang_String", "cn1_com_codename1_impl_html5_HTML5Implementation_getBrowserLanguage___R_java_lang_String"], function*() {
  const nav = global.navigator || {};
  const value = nav.language || nav.browserLanguage || "";
  return jvm.createStringLiteral(String(value));
});

bindNative([
  "cn1_com_codename1_impl_html5_HTML5Implementation_isWeakMapSupported_R_boolean",
  "cn1_com_codename1_impl_html5_HTML5Implementation_isWeakMapSupported___R_boolean"
], function*() {
  return typeof WeakMap === "function" ? 1 : 0;
});

bindNative([
  "cn1_com_codename1_impl_html5_HTML5Implementation_createSoftWeakRefImpl_com_codename1_html5_js_JSObject_R_com_codename1_html5_js_JSObject",
  "cn1_com_codename1_impl_html5_HTML5Implementation_createSoftWeakRefImpl___com_codename1_html5_js_JSObject_R_com_codename1_html5_js_JSObject"
], function*(objectRef) {
  if (typeof WeakMap !== "function") {
    return null;
  }
  const win = global.window || global;
  if (!(win.cn1GlobalWeakMap instanceof WeakMap)) {
    win.cn1GlobalWeakMap = new WeakMap();
  }
  const key = {};
  win.cn1GlobalWeakMap.set(key, jvm.unwrapJsValue(objectRef));
  return jvm.wrapJsObject(key, "com_codename1_html5_js_JSObject");
});

bindNative([
  "cn1_com_codename1_impl_html5_HTML5Implementation_extractHardRefImpl_com_codename1_html5_js_JSObject_R_com_codename1_html5_js_JSObject",
  "cn1_com_codename1_impl_html5_HTML5Implementation_extractHardRefImpl___com_codename1_html5_js_JSObject_R_com_codename1_html5_js_JSObject"
], function*(keyRef) {
  if (typeof WeakMap !== "function") {
    return null;
  }
  const win = global.window || global;
  const weakMap = win.cn1GlobalWeakMap;
  if (!(weakMap instanceof WeakMap)) {
    return null;
  }
  const key = jvm.unwrapJsValue(keyRef);
  if (key == null || !weakMap.has(key)) {
    return null;
  }
  const value = weakMap.get(key);
  return value == null ? null : jvm.wrapJsObject(value, jvm.inferJsObjectClass(value, null));
});

bindNative(["cn1_com_codename1_impl_html5_HTML5Implementation_debugFlag_java_lang_String_R_boolean", "cn1_com_codename1_impl_html5_HTML5Implementation_debugFlag___java_lang_String_R_boolean"], function*(name) {
  const win = global.window || global;
  const flags = win.cn1_debug_flags;
  if (!flags) {
    return 0;
  }
  return flags[jvm.toNativeString(name)] ? 1 : 0;
});

bindNative(["cn1_com_codename1_impl_html5_HTML5Implementation_getWheelEventType_R_java_lang_String", "cn1_com_codename1_impl_html5_HTML5Implementation_getWheelEventType___R_java_lang_String"], function*() {
  const win = global.window || global;
  const normalizeWheel = win.cn1NormalizeWheel;
  let value = "wheel";
  if (normalizeWheel && typeof normalizeWheel.getEventType === "function") {
    try {
      value = normalizeWheel.getEventType() || value;
    } catch (e) {}
  }
  return jvm.createStringLiteral(String(value));
});

bindNative([
  "cn1_com_codename1_impl_html5_HTML5Implementation_notifyProgressLoaderThatResourceIsLoaded_java_lang_String",
  "cn1_com_codename1_impl_html5_HTML5Implementation_notifyProgressLoaderThatResourceIsLoaded___java_lang_String"
], function*(resource) {
  const win = global.window || global;
  const handler = win.cn1LoadedFile;
  if (typeof handler === "function") {
    try {
      handler(String(jvm.toNativeString(resource)));
    } catch (e) {}
  }
  return null;
});

bindNative([
  "cn1_com_codename1_impl_html5_HTML5Implementation_installBeforeUnload",
  "cn1_com_codename1_impl_html5_HTML5Implementation_installBeforeUnload__"
], function*() {
  const win = global.window || global;
  win.onbeforeunload = function() {
    return "Leaving or refreshing the page may cause you to lose unsaved data.";
  };
  return null;
});

bindNative([
  "cn1_com_codename1_impl_html5_HTML5Implementation_getBeforeUnloadHandler_R_com_codename1_html5_js_JSObject",
  "cn1_com_codename1_impl_html5_HTML5Implementation_getBeforeUnloadHandler___R_com_codename1_html5_js_JSObject"
], function*() {
  const win = global.window || global;
  const handler = win.onbeforeunload;
  return handler == null ? null : jvm.wrapJsObject(handler, "com_codename1_html5_js_JSObject");
});

bindNative([
  "cn1_com_codename1_impl_html5_HTML5Implementation_setBeforeUnloadHandler_com_codename1_html5_js_JSObject",
  "cn1_com_codename1_impl_html5_HTML5Implementation_setBeforeUnloadHandler___com_codename1_html5_js_JSObject"
], function*(handler) {
  const win = global.window || global;
  win.onbeforeunload = handler == null ? null : jvm.unwrapJsValue(handler);
  return null;
});

bindNative([
  "cn1_com_codename1_impl_html5_HTML5Implementation_setBeforeUnloadMessage_java_lang_String",
  "cn1_com_codename1_impl_html5_HTML5Implementation_setBeforeUnloadMessage___java_lang_String"
], function*(msg) {
  const win = global.window || global;
  const value = msg == null ? "" : jvm.toNativeString(msg);
  win.onbeforeunload = function() {
    return value;
  };
  return null;
});

bindNative([
  "cn1_com_codename1_impl_html5_HTML5Implementation_removeBeforeUnload",
  "cn1_com_codename1_impl_html5_HTML5Implementation_removeBeforeUnload__"
], function*() {
  const win = global.window || global;
  win.onbeforeunload = function() {};
  return null;
});

bindNative([
  "cn1_com_codename1_teavm_io_BlobUtil_installNativeBlobToFileConverter_com_codename1_teavm_io_BlobUtil_BlobToFileFunc",
  "cn1_com_codename1_teavm_io_BlobUtil_installNativeBlobToFileConverter___com_codename1_teavm_io_BlobUtil_BlobToFileFunc"
], function*(_func) {
  const win = global.window || global;
  win.saveBlobToFile = function(_blob, _fileName, callback) {
    if (callback && typeof callback.error === "function") {
      callback.error("Blob-to-file conversion is not implemented in the ParparVM runtime yet.");
    }
  };
  return null;
});

bindCiFallback("BlobUtil.canvasToBlobDirect", [
  "cn1_com_codename1_teavm_io_BlobUtil_canvasToBlob_com_codename1_html5_js_dom_HTMLCanvasElement_java_lang_String_double_R_com_codename1_teavm_jso_io_Blob",
  "cn1_com_codename1_teavm_io_BlobUtil_canvasToBlob_com_codename1_html5_js_dom_HTMLCanvasElement_java_lang_String_double_R_com_codename1_teavm_jso_io_Blob__impl"
], function*(canvas, mimeType, quality) {
  const nativeCanvas = jvm.unwrapJsValue(canvas) || canvas;
  const mime = mimeType && mimeType.__class === "java_lang_String"
    ? jvm.toNativeString(mimeType)
    : (typeof mimeType === "string" ? mimeType : "image/png");
  const q = typeof quality === "number" ? quality : 0.92;
  let dataUrl = "";
  if (nativeCanvas && nativeCanvas.__cn1HostRef != null && typeof jvm.invokeHostNative === "function") {
    try {
      dataUrl = yield jvm.invokeHostNative("__cn1_jso_bridge__", [{
        receiver: nativeCanvas,
        kind: "method",
        member: "toDataURL",
        args: [mime || "image/png", q]
      }]);
    } catch (_err) {
      dataUrl = yield jvm.invokeHostNative("__cn1_jso_bridge__", [{
        receiver: nativeCanvas,
        kind: "method",
        member: "toDataURL",
        args: ["image/png"]
      }]);
    }
  } else if (!nativeCanvas || typeof nativeCanvas.toDataURL !== "function") {
    emitDiagLine("PARPAR:DIAG:FALLBACK:blobUtilCanvasToBlob:missingCanvas=1");
    return null;
  } else {
    try {
      dataUrl = nativeCanvas.toDataURL(mime || "image/png", q);
    } catch (_err) {
      dataUrl = nativeCanvas.toDataURL("image/png");
    }
  }
  const comma = dataUrl.indexOf(",");
  if (comma < 0) {
    return null;
  }
  const header = dataUrl.substring(0, comma);
  const payload = dataUrl.substring(comma + 1);
  const match = /data:([^;]+);base64/i.exec(header);
  const contentType = match && match[1] ? match[1] : (mime || "image/png");
  const decoded = global.atob ? global.atob(payload) : "";
  const bytes = new global.Uint8Array(decoded.length);
  for (let i = 0; i < decoded.length; i++) {
    bytes[i] = decoded.charCodeAt(i) & 0xff;
  }
  const blob = new global.Blob([bytes], { type: contentType });
  const wrappedBlob = jvm.wrapJsObject(blob, "com_codename1_teavm_jso_io_Blob");
  wrappedBlob.__cn1BlobBytes = bytes;
  return wrappedBlob;
});

bindCiFallback("BlobUtil.toUint8ArrayDirect", [
  "cn1_com_codename1_teavm_io_BlobUtil_toUint8Array_com_codename1_teavm_jso_io_Blob_R_com_codename1_html5_js_typedarrays_Uint8Array",
  "cn1_com_codename1_teavm_io_BlobUtil_toUint8Array_com_codename1_teavm_jso_io_Blob_R_com_codename1_html5_js_typedarrays_Uint8Array__impl"
], function*(blob) {
  if (blob && blob.__cn1BlobBytes) {
    return jvm.wrapJsObject(new global.Uint8Array(blob.__cn1BlobBytes), "com_codename1_html5_js_typedarrays_Uint8Array");
  }
  const nativeBlob = jvm.unwrapJsValue(blob);
  if (nativeBlob && nativeBlob.__cn1BlobBytes) {
    return jvm.wrapJsObject(new global.Uint8Array(nativeBlob.__cn1BlobBytes), "com_codename1_html5_js_typedarrays_Uint8Array");
  }
  emitDiagLine("PARPAR:DIAG:FALLBACK:blobUtilToUint8Array:empty=1");
  return jvm.wrapJsObject(new global.Uint8Array(0), "com_codename1_html5_js_typedarrays_Uint8Array");
});

bindNative([
  "cn1_com_codename1_impl_html5_HTML5Implementation_requestAnimationFrameNative_com_codename1_impl_html5_JavaScriptAnimationFrameCallback_R_int",
  "cn1_com_codename1_impl_html5_HTML5Implementation_requestAnimationFrameNative___com_codename1_impl_html5_JavaScriptAnimationFrameCallback_R_int"
], function*(handler) {
  const win = global.window || global;
  return (win.requestAnimationFrame || function(cb) { return win.setTimeout(function() { cb(Date.now()); }, 16); })(function(time) {
    try {
      spawnVirtualCallback(
        handler,
        "cn1_com_codename1_impl_html5_JavaScriptAnimationFrameCallback_onAnimationFrame_double",
        [+time],
        "__cn1RafCallbackPending"
      );
    } catch (err) {
      jvm.fail(err);
    }
  }) |0;
});

bindCiFallback("Display.setProperty", [
  "cn1_com_codename1_ui_Display_setProperty_java_lang_String_java_lang_String"
], function*(__cn1ThisObject, key, value) {
  if (!__cn1ThisObject) {
    return null;
  }
  const map = __cn1ThisObject.__cn1RuntimeProperties || (__cn1ThisObject.__cn1RuntimeProperties = Object.create(null));
  const k = key == null ? "" : jvm.toNativeString(key);
  if (value == null) {
    delete map[k];
  } else {
    map[k] = jvm.toNativeString(value);
  }
  return null;
});

// The JSAffineTransform matrix is stored as { m00, m10, m01, m11, m02, m12 }
// — plain fields, no accessor methods. The earlier version of this fallback
// called getScaleX()/getShearY()/etc. (mirroring the JS-side getter methods
// that used to exist) and broke the moment that JS wrapper was simplified to
// field-only storage. Read the six fields directly to match the Java-side
// @JSBody scripts in JSAffineTransform.JSOFactory.
bindCiFallback("JSAffineTransform.setTransformHostBridge", [
  "cn1_com_codename1_teavm_geom_JSAffineTransform_JSOFactory_setTransform_com_codename1_html5_js_canvas_CanvasRenderingContext2D_com_codename1_teavm_geom_JSAffineTransform_JSOAffineTransform",
  "cn1_com_codename1_teavm_geom_JSAffineTransform_JSOFactory_setTransform_com_codename1_html5_js_canvas_CanvasRenderingContext2D_com_codename1_teavm_geom_JSAffineTransform_JSOAffineTransform__impl"
], function*(context, transform) {
  const nativeContext = jvm.unwrapJsValue(context) || context;
  const nativeTransform = jvm.unwrapJsValue(transform) || transform;
  const args = [
    nativeTransform.m00,
    nativeTransform.m10,
    nativeTransform.m01,
    nativeTransform.m11,
    nativeTransform.m02,
    nativeTransform.m12
  ];
  if (nativeContext && nativeContext.__cn1HostRef != null && typeof jvm.invokeHostNative === "function") {
    yield jvm.invokeHostNative("__cn1_jso_bridge__", [{
      receiver: nativeContext,
      kind: "method",
      member: "setTransform",
      args
    }]);
    return null;
  }
  nativeContext.setTransform.apply(nativeContext, args);
  return null;
});

bindCiFallback("JSAffineTransform.transformHostBridge", [
  "cn1_com_codename1_teavm_geom_JSAffineTransform_JSOFactory_transform_com_codename1_html5_js_canvas_CanvasRenderingContext2D_com_codename1_teavm_geom_JSAffineTransform_JSOAffineTransform",
  "cn1_com_codename1_teavm_geom_JSAffineTransform_JSOFactory_transform_com_codename1_html5_js_canvas_CanvasRenderingContext2D_com_codename1_teavm_geom_JSAffineTransform_JSOAffineTransform__impl"
], function*(context, transform) {
  const nativeContext = jvm.unwrapJsValue(context) || context;
  const nativeTransform = jvm.unwrapJsValue(transform) || transform;
  const args = [
    nativeTransform.m00,
    nativeTransform.m10,
    nativeTransform.m01,
    nativeTransform.m11,
    nativeTransform.m02,
    nativeTransform.m12
  ];
  if (nativeContext && nativeContext.__cn1HostRef != null && typeof jvm.invokeHostNative === "function") {
    yield jvm.invokeHostNative("__cn1_jso_bridge__", [{
      receiver: nativeContext,
      kind: "method",
      member: "transform",
      args
    }]);
    return null;
  }
  nativeContext.transform.apply(nativeContext, args);
  return null;
});

// Route createCrossOriginImageElement through the main-thread image decoder
// so the worker receives an already-decoded <img>. The stock Java flow
// (createElement + setSrc + return) returns before the browser has actually
// fetched/decoded the image, so NativeImage.isComplete() reports false on
// the first paint and Border.paintBorderBackground ends up painting only
// the first 9-patch piece whose blob happened to decode fastest. Awaiting
// img.decode() here makes the subsequent draws have a ready pixel source.
bindCiFallback("BrowserDomRenderingBackend.createCrossOriginImageElement", [
  "cn1_com_codename1_impl_html5_HTML5Implementation_BrowserDomRenderingBackend_createCrossOriginImageElement_java_lang_String_R_com_codename1_html5_js_dom_HTMLImageElement"
], function*(__cn1ThisObject, sourceUrl) {
  const url = sourceUrl == null ? null : jvm.toNativeString(sourceUrl);
  if (!url) {
    return null;
  }
  if (typeof jvm.invokeHostNative !== "function") {
    return null;
  }
  const hostImage = yield jvm.invokeHostNative("__cn1_decode_image_from_url__", [{
    sourceUrl: url,
    crossOrigin: "anonymous"
  }]);
  if (hostImage == null) {
    return null;
  }
  return jvm.wrapJsObject(hostImage, "com_codename1_html5_js_dom_HTMLImageElement");
});

bindCiFallback("Display.getProperty", [
  "cn1_com_codename1_ui_Display_getProperty_java_lang_String_java_lang_String_R_java_lang_String"
], function*(__cn1ThisObject, key, defaultValue) {
  const map = __cn1ThisObject && __cn1ThisObject.__cn1RuntimeProperties;
  const k = key == null ? "" : jvm.toNativeString(key);
  if (map && Object.prototype.hasOwnProperty.call(map, k)) {
    return jvm.createStringLiteral(map[k]);
  }
  return defaultValue || null;
});

bindCiFallback("Display.addEdtErrorHandler", [
  "cn1_com_codename1_ui_Display_addEdtErrorHandler_com_codename1_ui_events_ActionListener"
], function*(__cn1ThisObject, listener) {
  if (!__cn1ThisObject) {
    return null;
  }
  const handlers = __cn1ThisObject.__cn1EdtErrorHandlers || (__cn1ThisObject.__cn1EdtErrorHandlers = []);
  handlers.push(listener || null);
  return null;
});

bindCiFallback("Log.print", [
  "cn1_com_codename1_io_Log_print_java_lang_String_int"
], function*(__cn1ThisObject, message, level) {
  const text = message == null ? "" : jvm.toNativeString(message);
  if ((level | 0) >= 1 && global.console && typeof global.console.error === "function") {
    global.console.error(text);
  } else if (global.console && typeof global.console.log === "function") {
    global.console.log(text);
  }
  return null;
});

bindCiFallback("Log.e", [
  "cn1_com_codename1_io_Log_e_java_lang_Throwable"
], function*(__cn1ThisObject, throwable) {
  if (global.console && typeof global.console.error === "function") {
    global.console.error("Exception: " + (yield* stringifyThrowable(throwable)));
  }
  return null;
});

bindCiFallback("NetworkManager.addErrorListener", [
  "cn1_com_codename1_io_NetworkManager_addErrorListener_com_codename1_ui_events_ActionListener"
], function*(__cn1ThisObject, listener) {
  if (!__cn1ThisObject) {
    return null;
  }
  const handlers = __cn1ThisObject.__cn1NetworkErrorListeners || (__cn1ThisObject.__cn1NetworkErrorListeners = []);
  handlers.push(listener || null);
  return null;
});

// Worker-safe implementation of HTML5Implementation.loadTrueTypeFont_: the
// @JSBody version expands to document.createElement + WebFont.load, which has
// no hope of running in the worker-only runtime. Route to the host via the
// __cn1_load_truetype_font__ bridge so the returned promise suspends the
// generator until the host actually has the font available to CSS. The
// worker passes the bare resource name (e.g. material-design-font.ttf); the
// host mirrors HTML5Implementation.getResourceAsStream and resolves it to
// assets/<name> before handing it to FontFace. We avoid the previous
// arrayBuffer->base64 dataURL route because Window.current().arrayBufferToBase64
// is not wired up in the worker and silently returned an empty string.
bindNative([
  "cn1_com_codename1_impl_html5_HTML5Implementation_loadTrueTypeFont__java_lang_String_java_lang_String_java_lang_String"
], function*(fontName, fontFile, fontFormat) {
  const toStr = function(v) {
    if (v == null) return "";
    return typeof v === "string" ? v : (jvm.toNativeString ? jvm.toNativeString(v) : String(v));
  };
  const payload = {
    fontName: toStr(fontName),
    fontUrl: toStr(fontFile),
    fontFormat: toStr(fontFormat) || "truetype"
  };
  try {
    yield jvm.invokeHostNative("__cn1_load_truetype_font__", [payload]);
  } catch (err) {
    if (global.console && typeof global.console.warn === "function") {
      global.console.warn("PARPAR:DIAG:loadTrueTypeFont:hostBridgeError=" + (err && err.message ? err.message : err));
    }
  }
  return null;
});

const nativeFontGetCssMethodId = "cn1_com_codename1_impl_html5_HTML5Implementation_NativeFont_getCSS_R_java_lang_String";
const nativeFontCharWidthMethodId = "cn1_com_codename1_impl_html5_HTML5Implementation_NativeFont_charWidth_char_R_int";

// Resolve the translated NativeFont methods lazily. bindCiFallback captures the
// original symbol at port.js evaluation time, which runs before the translated
// class metadata is attached to jvm.classes - so a top-level lookup returns
// null and the fallback silently returns "16px sans-serif" for every font,
// stripping the 'Material Icons' (or any) family from the CSS. bindNative
// preserves the pre-override function in jvm.translatedMethods, so prefer that
// path. Fall back to the __impl global (which is not replaced by bindNative).
// Never read back from global[methodId] itself or jvm.classes[..].methods[id]
// because bindCiFallback has overwritten those with this very fallback - that
// would be an infinite recursion (was the cause of a Maximum call stack size
// exceeded during the first attempt at lazy resolution).
function resolveNativeFontOriginal(methodId) {
  if (jvm.translatedMethods && typeof jvm.translatedMethods[methodId] === "function") {
    return jvm.translatedMethods[methodId];
  }
  const implKey = methodId + "__impl";
  if (typeof global[implKey] === "function" && !global[implKey].__cn1CiFallbackSymbol) {
    return global[implKey];
  }
  return null;
}

bindCiFallback("NativeFont.getCSSNullSafe", [
  nativeFontGetCssMethodId
], function*(__cn1ThisObject) {
  const original = resolveNativeFontOriginal(nativeFontGetCssMethodId);
  if (typeof original !== "function") {
    return jvm.createStringLiteral("16px sans-serif");
  }
  try {
    return yield* original(__cn1ThisObject);
  } catch (err) {
    const message = String(err && err.message ? err.message : err || "");
    if (message.indexOf("__classDef") >= 0) {
      emitCiFallbackMarker("NativeFont.getCSSNullReceiver", "HIT");
      return jvm.createStringLiteral("16px sans-serif");
    }
    throw err;
  }
});

bindCiFallback("NativeFont.charWidthNullSafe", [
  nativeFontCharWidthMethodId
], function*(__cn1ThisObject, chr) {
  const original = resolveNativeFontOriginal(nativeFontCharWidthMethodId);
  if (typeof original !== "function") {
    return 8;
  }
  try {
    return yield* original(__cn1ThisObject, chr);
  } catch (err) {
    emitCiFallbackMarker("NativeFont.charWidthDefaulted", "HIT");
    return 8;
  }
});

bindCiFallback("HTML5Graphics.colorWithAlphaDirect", [
  "cn1_com_codename1_impl_html5_HTML5Graphics_colorWithAlpha_int_R_java_lang_String",
  "cn1_com_codename1_impl_html5_HTML5Graphics_colorWithAlpha_int_R_java_lang_String__impl"
], function*(argb) {
  const value = argb | 0;
  const r = (value >>> 16) & 0xff;
  const g = (value >>> 8) & 0xff;
  const b = value & 0xff;
  const a = ((value >>> 24) & 0xff) / 255;
  return jvm.createStringLiteral("rgba(" + r + "," + g + "," + b + "," + a + ")");
});

const determineFontHeightMethodId = "cn1_com_codename1_impl_html5_HTML5Implementation_determineFontHeight_java_lang_String_R_double";
const determineFontHeightImplMethodId = "cn1_com_codename1_impl_html5_HTML5Implementation_determineFontHeight_java_lang_String_R_double__impl";
const determineFontHeightOriginal = typeof global[determineFontHeightImplMethodId] === "function"
  ? global[determineFontHeightImplMethodId]
  : (typeof global[determineFontHeightMethodId] === "function" ? global[determineFontHeightMethodId] : null);

bindCiFallback("HTML5Implementation.determineFontHeightCoerce", [
  determineFontHeightImplMethodId,
  determineFontHeightMethodId
], function*(fontStyle) {
  if (typeof determineFontHeightOriginal === "function") {
    try {
      return yield* determineFontHeightOriginal(fontStyle);
    } catch (err) {
      const message = String(err && err.message ? err.message : err || "");
      if (message.indexOf("indexOf is not a function") < 0) {
        throw err;
      }
    }
  }
  let css = "";
  if (fontStyle && fontStyle.__class === "java_lang_String") {
    css = jvm.toNativeString(fontStyle);
  } else if (typeof fontStyle === "string") {
    css = fontStyle;
  } else if (fontStyle != null) {
    css = String(fontStyle);
  }
  const match = /([0-9]+(?:\.[0-9]+)?)\s*(px|pt)/i.exec(css);
  if (match) {
    const value = parseFloat(match[1]);
    if (!isNaN(value) && value > 0) {
      return value;
    }
  }
  return 16.0;
});

const hashMapComputeHashCodeImplMethodId = "cn1_java_util_HashMap_computeHashCode_java_lang_Object_R_int__impl";
const hashMapComputeHashCodeMethodId = "cn1_java_util_HashMap_computeHashCode_java_lang_Object_R_int";
const hashMapComputeHashCodeOriginal = typeof global[hashMapComputeHashCodeImplMethodId] === "function"
  ? global[hashMapComputeHashCodeImplMethodId]
  : (typeof global[hashMapComputeHashCodeMethodId] === "function" ? global[hashMapComputeHashCodeMethodId] : null);

bindCiFallback("HashMap.computeHashCodeNullKey", [
  hashMapComputeHashCodeImplMethodId,
  hashMapComputeHashCodeMethodId
], function*(key) {
  if (key == null) {
    emitDiagLine("PARPAR:DIAG:FALLBACK:hashMapComputeHashCode:nullKey=1");
    return 0;
  }
  // Try the original captured at port.js load time first.
  if (typeof hashMapComputeHashCodeOriginal === "function") {
    return yield* hashMapComputeHashCodeOriginal(key);
  }
  // Original wasn't available yet (translated_app.js loads after port.js).
  // computeHashCode(key) is just key.hashCode(), so call hashCode directly
  // via virtual dispatch to avoid recursion back into computeHashCode.
  var hashCodeMethod = jvm.resolveVirtual(key.__class || "java_lang_Object",
    "cn1_java_lang_Object_hashCode_R_int");
  if (typeof hashCodeMethod === "function") {
    return yield* hashCodeMethod(key);
  }
  return 0;
});
if (typeof global[hashMapComputeHashCodeImplMethodId] === "function") {
  const originalHashMapComputeHashCodeImpl = global[hashMapComputeHashCodeImplMethodId];
  global[hashMapComputeHashCodeImplMethodId] = function*(key) {
    if (key == null) {
      emitDiagLine("PARPAR:DIAG:FALLBACK:hashMapComputeHashCodeDirect:nullKey=1");
      return 0;
    }
    return yield* originalHashMapComputeHashCodeImpl(key);
  };
  emitDiagLine("PARPAR:DIAG:INIT:shim=hashMapComputeHashCodeImplNullKey");
}
if (typeof global[hashMapComputeHashCodeMethodId] === "function") {
  const originalHashMapComputeHashCode = global[hashMapComputeHashCodeMethodId];
  global[hashMapComputeHashCodeMethodId] = function*(key) {
    if (key == null) {
      emitDiagLine("PARPAR:DIAG:FALLBACK:hashMapComputeHashCodeDirect:nullKey=1");
      return 0;
    }
    return yield* originalHashMapComputeHashCode(key);
  };
  emitDiagLine("PARPAR:DIAG:INIT:shim=hashMapComputeHashCodeNullKey");
}
const hashMapClassDef = jvm.classes && jvm.classes["java_util_HashMap"];
if (hashMapClassDef && hashMapClassDef.methods && typeof hashMapClassDef.methods[hashMapComputeHashCodeMethodId] === "function") {
  const originalClassHashMapComputeHashCode = hashMapClassDef.methods[hashMapComputeHashCodeMethodId];
  hashMapClassDef.methods[hashMapComputeHashCodeMethodId] = function*(__cn1ThisObject, key) {
    if (key == null) {
      emitDiagLine("PARPAR:DIAG:FALLBACK:hashMapComputeHashCodeClass:nullKey=1");
      return 0;
    }
    return yield* originalClassHashMapComputeHashCode(__cn1ThisObject, key);
  };
  emitDiagLine("PARPAR:DIAG:INIT:shim=hashMapComputeHashCodeClassNullKey");
}

const styleSetPaddingUnitMethodId = "cn1_com_codename1_ui_plaf_Style_setPaddingUnit_byte_1ARRAY";
const styleSetMarginUnitMethodId = "cn1_com_codename1_ui_plaf_Style_setMarginUnit_byte_1ARRAY";
const styleConvertUnitMethodId = "cn1_com_codename1_ui_plaf_Style_convertUnit_byte_1ARRAY_float_int_R_int";
// The translator-generated method functions aren't all registered yet at
// port.js evaluation time, so capturing a snapshot here can leave these
// originals null. When the fallback below then short-circuits to `return 0`,
// every Style.getMarginTop()/getPaddingTop() call returns 0 — which
// collapses every layout's margin/padding and in particular leaves the
// Picker's InteractionDialog filling the full layered pane instead of
// anchoring to the bottom. Resolve the original lazily at call time
// against the jvm.translatedMethods map (populated by bindNative before it
// replaces the global), with fallbacks so late registrations still work.
function resolveTranslatedMethod(className, methodId) {
  if (jvm && jvm.translatedMethods && typeof jvm.translatedMethods[methodId] === "function") {
    return jvm.translatedMethods[methodId];
  }
  if (jvm && jvm.classes && jvm.classes[className] && jvm.classes[className].methods) {
    const method = jvm.classes[className].methods[methodId];
    if (typeof method === "function") {
      return method;
    }
  }
  if (typeof global[methodId] === "function" && !global[methodId].__cn1CiFallbackSymbol) {
    return global[methodId];
  }
  return null;
}

function ensureJavaByteArray4(value) {
  if (value && value.__array) {
    if ((value.length | 0) >= 4) {
      return value;
    }
    const outArr = jvm.newArray(4, "JAVA_BYTE", 1);
    for (let i = 0; i < 4; i++) {
      outArr[i] = i < value.length ? (value[i] | 0) : 0;
    }
    return outArr;
  }
  const out = jvm.newArray(4, "JAVA_BYTE", 1);
  if (Array.isArray(value)) {
    for (let i = 0; i < 4; i++) {
      out[i] = i < value.length ? (value[i] | 0) : 0;
    }
    return out;
  }
  const scalar = value == null ? 0 : (value | 0);
  for (let i = 0; i < 4; i++) {
    out[i] = scalar;
  }
  return out;
}

function installGlobalArrayReturnCoerce(symbol, className, marker) {
  const original = global[symbol];
  if (typeof original !== "function" || original.__cn1ArrayReturnCoerceWrapped) {
    return false;
  }
  const wrapped = function*() {
    const result = yield* original.apply(this, arguments);
    const coerced = ensureJavaByteArray4(result);
    if (result !== coerced) {
      emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":coerced=1");
    }
    return coerced;
  };
  wrapped.__cn1ArrayReturnCoerceWrapped = true;
  global[symbol] = wrapped;
  if (jvm && jvm.classes && jvm.classes[className] && jvm.classes[className].methods && typeof jvm.classes[className].methods[symbol] === "function") {
    jvm.classes[className].methods[symbol] = wrapped;
  }
  emitCiFallbackMarker(marker, "ENABLED");
  return true;
}

bindCiFallback("Style.setPaddingUnitArrayCoerce", [
  styleSetPaddingUnitMethodId
], function*(__cn1ThisObject, arr) {
  const original = resolveTranslatedMethod("com_codename1_ui_plaf_Style", styleSetPaddingUnitMethodId);
  if (typeof original !== "function") {
    return null;
  }
  return yield* original(__cn1ThisObject, ensureJavaByteArray4(arr));
});

bindCiFallback("Style.setMarginUnitArrayCoerce", [
  styleSetMarginUnitMethodId
], function*(__cn1ThisObject, arr) {
  const original = resolveTranslatedMethod("com_codename1_ui_plaf_Style", styleSetMarginUnitMethodId);
  if (typeof original !== "function") {
    return null;
  }
  return yield* original(__cn1ThisObject, ensureJavaByteArray4(arr));
});

bindCiFallback("Style.convertUnitArrayCoerce", [
  styleConvertUnitMethodId
], function*(__cn1ThisObject, arr, value, side) {
  const original = resolveTranslatedMethod("com_codename1_ui_plaf_Style", styleConvertUnitMethodId);
  if (typeof original !== "function") {
    // Null unit means PIXELS in CN1; for any other byte value the translator
    // already normalises through the original body, but if we can't find it
    // at all, at least behave like pixels for the null-unit path instead of
    // collapsing every margin/padding getter to zero.
    if (arr == null || !arr.__array) {
      return value | 0;
    }
    return 0;
  }
  return yield* original(__cn1ThisObject, ensureJavaByteArray4(arr), value, side);
});

installGlobalArrayReturnCoerce(
  "cn1_com_codename1_ui_plaf_Style_getPaddingUnit_R_byte_1ARRAY",
  "com_codename1_ui_plaf_Style",
  "Style.getPaddingUnitReturnCoerce"
);
installGlobalArrayReturnCoerce(
  "cn1_com_codename1_ui_plaf_Style_getMarginUnit_R_byte_1ARRAY",
  "com_codename1_ui_plaf_Style",
  "Style.getMarginUnitReturnCoerce"
);

const formInitLafMethodId = "cn1_com_codename1_ui_Form_initLaf_com_codename1_ui_plaf_UIManager";
const formInitFocusedMethodId = "cn1_com_codename1_ui_Form_initFocused";
const formFlushRevalidateQueueMethodId = "cn1_com_codename1_ui_Form_flushRevalidateQueue";
const formDeinitializeImplMethodId = "cn1_com_codename1_ui_Form_deinitializeImpl";
const formGetActualPaneMethodId = "cn1_com_codename1_ui_Form_getActualPane_R_com_codename1_ui_Container";
const formSetFocusedMethodId = "cn1_com_codename1_ui_Form_setFocused_com_codename1_ui_Component";
const formLayoutContainerMethodId = "cn1_com_codename1_ui_Form_layoutContainer";
const containerFindFirstFocusableMethodId = "cn1_com_codename1_ui_Container_findFirstFocusable_R_com_codename1_ui_Component";
const displayGetInstanceMethodId = "cn1_com_codename1_ui_Display_getInstance_R_com_codename1_ui_Display";
const displayShouldRenderSelectionMethodId = "cn1_com_codename1_ui_Display_shouldRenderSelection_R_boolean";
let formInitLafDiagCount = 0;
function emitFormInitLafDiag(line) {
  if (formInitLafDiagCount >= 80) {
    return;
  }
  formInitLafDiagCount++;
  emitDiagLine(line);
}
function isCiFallbackFunction(candidate, fallbackSymbol) {
  return !!(candidate && candidate.__cn1CiFallbackSymbol === fallbackSymbol);
}
function resolveCurrentTranslatedMethod(methodIds, ownerClassName, fallbackSymbol) {
  const ids = Array.isArray(methodIds) ? methodIds : [methodIds];
  const translatedMethods = jvm && jvm.translatedMethods ? jvm.translatedMethods : null;
  if (translatedMethods) {
    for (let i = 0; i < ids.length; i++) {
      const candidate = translatedMethods[ids[i]];
      if (typeof candidate === "function" && !isCiFallbackFunction(candidate, fallbackSymbol)) {
        return candidate;
      }
    }
  }
  const ownerClass = jvm && jvm.classes ? jvm.classes[ownerClassName] : null;
  const methods = ownerClass && ownerClass.methods ? ownerClass.methods : null;
  if (methods) {
    for (let i = 0; i < ids.length; i++) {
      const candidate = methods[ids[i]];
      if (typeof candidate === "function" && !isCiFallbackFunction(candidate, fallbackSymbol)) {
        return candidate;
      }
    }
  }
  for (let i = 0; i < ids.length; i++) {
    const candidate = global[ids[i]];
    if (typeof candidate === "function" && !isCiFallbackFunction(candidate, fallbackSymbol)) {
      return candidate;
    }
  }
  return null;
}
function isLikelyFormObject(value) {
  if (!value || typeof value !== "object") {
    return false;
  }
  const classId = value.__class ? String(value.__class) : "";
  if (classId.indexOf("com_codename1_ui_Form") === 0 || classId.indexOf("com_codename1_ui_Dialog") === 0) {
    return true;
  }
  const classDef = value.__classDef;
  if (classDef && classDef.assignableTo) {
    if (classDef.assignableTo["com_codename1_ui_Form"] || classDef.assignableTo["com_codename1_ui_Dialog"]) {
      return true;
    }
  }
  if (typeof jvm.instanceOf === "function") {
    try {
      return jvm.instanceOf(value, "com_codename1_ui_Form") || jvm.instanceOf(value, "com_codename1_ui_Dialog");
    } catch (_err) {
      return false;
    }
  }
  return false;
}

function* safeInitLafPath(form, uiManager, lookAndFeel) {
  const containerInitLaf = global.cn1_com_codename1_ui_Container_initLaf_com_codename1_ui_plaf_UIManager;
  if (typeof containerInitLaf === "function") {
    yield* containerInitLaf(form, uiManager);
  }
  let effectiveLookAndFeel = lookAndFeel || null;
  if (!effectiveLookAndFeel && uiManager && uiManager.__class) {
    try {
      const getLookAndFeel = jvm.resolveVirtual(
        uiManager.__class,
        "cn1_com_codename1_ui_plaf_UIManager_getLookAndFeel_R_com_codename1_ui_plaf_LookAndFeel"
      );
      effectiveLookAndFeel = yield* getLookAndFeel(uiManager);
    } catch (_err) {
      effectiveLookAndFeel = null;
    }
  }
  let menuBar = form["cn1_com_codename1_ui_Form_menuBar"] || null;
  if (!menuBar) {
    const menuBarCtor = global.cn1_com_codename1_ui_MenuBar___INIT____impl
      || global.cn1_com_codename1_ui_MenuBar___INIT__;
    if (typeof menuBarCtor === "function") {
      menuBar = jvm.newObject("com_codename1_ui_MenuBar");
      yield* menuBarCtor(menuBar);
      form["cn1_com_codename1_ui_Form_menuBar"] = menuBar;
    }
  }
  if (menuBar && menuBar.__class) {
    try {
      const initMenuBar = jvm.resolveVirtual(
        menuBar.__class,
        "cn1_com_codename1_ui_MenuBar_initMenuBar_com_codename1_ui_Form"
      );
      yield* initMenuBar(menuBar, form);
    } catch (_err) {
      // best effort
    }
  }
  if (effectiveLookAndFeel && effectiveLookAndFeel.__class) {
    try {
      const getTint = jvm.resolveVirtual(
        effectiveLookAndFeel.__class,
        "cn1_com_codename1_ui_plaf_LookAndFeel_getDefaultFormTintColor_R_int"
      );
      form["cn1_com_codename1_ui_Form_tintColor"] = yield* getTint(effectiveLookAndFeel);
    } catch (_err) {
      // best effort
    }
  }
  return null;
}

function* recoverInitFocusedNullReceiver(form) {
  if (!form || !form.__class) {
    emitDiagLine("PARPAR:DIAG:FALLBACK:formInitFocused:recoverSkipped=noForm");
    return null;
  }
  yield* ensureComponentBounds(form, "formInitFocused:self");
  yield* ensureContainerComponentsList(form, "formInitFocused:self");
  yield* ensureContainerLayout(form, true, "formInitFocused:self");
  yield* ensureFormRevalidateQueues(form, "formInitFocused:self");
  yield* ensureFormAnimationManager(form, "formInitFocused:self");
  let focusCandidate = null;
  const formLayeredPane = form["cn1_com_codename1_ui_Form_formLayeredPane"] || null;
  if (formLayeredPane && formLayeredPane.__class) {
    try {
      const findFirstFocusable = jvm.resolveVirtual(formLayeredPane.__class, containerFindFirstFocusableMethodId);
      focusCandidate = yield* findFirstFocusable(formLayeredPane);
    } catch (_err) {
      focusCandidate = null;
    }
  }
  if (!focusCandidate) {
    let pane = null;
    try {
      const getActualPane = jvm.resolveVirtual(form.__class, formGetActualPaneMethodId);
      pane = yield* getActualPane(form);
    } catch (_err) {
      pane = null;
    }
    if (!pane || !pane.__class) {
      pane = yield* ensureFormContentPane(form, "formInitFocused");
    }
    if (pane && pane.__class) {
      yield* ensureComponentBounds(pane, "formInitFocused:pane");
      yield* ensureContainerComponentsList(pane, "formInitFocused:pane");
      yield* ensureContainerLayout(pane, false, "formInitFocused:pane");
      try {
        const findFirstFocusable = jvm.resolveVirtual(pane.__class, containerFindFirstFocusableMethodId);
        focusCandidate = yield* findFirstFocusable(pane);
      } catch (_err) {
        focusCandidate = null;
      }
    }
  }
  try {
    const setFocused = jvm.resolveVirtual(form.__class, formSetFocusedMethodId);
    yield* setFocused(form, focusCandidate);
  } catch (_err) {
    form["cn1_com_codename1_ui_Form_focused"] = focusCandidate || null;
  }
  let shouldRenderSelection = 0;
  try {
    const getDisplay = global[displayGetInstanceMethodId + "__impl"] || global[displayGetInstanceMethodId];
    if (typeof getDisplay === "function") {
      const display = yield* getDisplay();
      if (display && display.__class) {
        const shouldRenderSelectionFn = jvm.resolveVirtual(display.__class, displayShouldRenderSelectionMethodId);
        shouldRenderSelection = (yield* shouldRenderSelectionFn(display)) | 0;
      }
    }
  } catch (_err) {
    shouldRenderSelection = 0;
  }
  if (shouldRenderSelection) {
    try {
      const layoutContainer = jvm.resolveVirtual(form.__class, formLayoutContainerMethodId);
      yield* layoutContainer(form);
    } catch (_err) {
      // Best effort.
    }
  }
  emitDiagLine(
    "PARPAR:DIAG:FALLBACK:formInitFocused:recoverApplied=1"
    + ":focus=" + (focusCandidate && focusCandidate.__class ? focusCandidate.__class : "null")
    + ":render=" + (shouldRenderSelection ? "1" : "0")
  );
  return null;
}

bindCiFallback("Form.initLafNullUiManagerBridge", [
  formInitLafMethodId
], function*(__cn1ThisObject, uiManager) {
  const formInitLafOriginalMethod = resolveCurrentTranslatedMethod(
    [formInitLafMethodId],
    "com_codename1_ui_Form",
    "Form.initLafNullUiManagerBridge"
  );
  let effectiveSelf = __cn1ThisObject;
  let effectiveUiManager = uiManager;
  if (!isLikelyFormObject(effectiveSelf)) {
    let remappedField = null;
    if (effectiveSelf && typeof effectiveSelf === "object") {
      const keys = Object.keys(effectiveSelf);
      for (let i = 0; i < keys.length; i++) {
        const key = keys[i];
        const value = effectiveSelf[key];
        if (isLikelyFormObject(value)) {
          effectiveSelf = value;
          remappedField = key;
          break;
        }
      }
    }
    if (remappedField) {
      emitFormInitLafDiag("PARPAR:DIAG:FALLBACK:formInitLaf:receiverRemap=" + remappedField);
    }
  }
  emitFormInitLafDiag(
    "PARPAR:DIAG:FALLBACK:formInitLaf:enter:self="
    + (effectiveSelf && effectiveSelf.__class ? effectiveSelf.__class : "null")
    + ":uiManager=" + (effectiveUiManager && effectiveUiManager.__class ? effectiveUiManager.__class : "null")
  );
  if (!isLikelyFormObject(effectiveSelf)) {
    emitFormInitLafDiag("PARPAR:DIAG:FALLBACK:formInitLaf:receiverStillNonForm=1");
    return null;
  }
  if (!effectiveUiManager) {
    emitFormInitLafDiag("PARPAR:DIAG:FALLBACK:formInitLaf:nullUiManager=1");
    const getInstance = global.cn1_com_codename1_ui_plaf_UIManager_getInstance_R_com_codename1_ui_plaf_UIManager__impl
      || global.cn1_com_codename1_ui_plaf_UIManager_getInstance_R_com_codename1_ui_plaf_UIManager;
    if (typeof getInstance === "function") {
      effectiveUiManager = yield* getInstance();
    }
  }
  if (!effectiveUiManager) {
    emitFormInitLafDiag("PARPAR:DIAG:FALLBACK:formInitLaf:uiManagerStillNull=1");
    return null;
  }
  let lookAndFeel = null;
  try {
    const getLookAndFeel = jvm.resolveVirtual(
      effectiveUiManager.__class,
      "cn1_com_codename1_ui_plaf_UIManager_getLookAndFeel_R_com_codename1_ui_plaf_LookAndFeel"
    );
    lookAndFeel = yield* getLookAndFeel(effectiveUiManager);
  } catch (_err) {
    lookAndFeel = null;
  }
  emitFormInitLafDiag(
    "PARPAR:DIAG:FALLBACK:formInitLaf:lookAndFeel="
    + (lookAndFeel && lookAndFeel.__class ? lookAndFeel.__class : "null")
  );
  if (!lookAndFeel) {
    const defaultLookAndFeelCtor = global.cn1_com_codename1_ui_plaf_DefaultLookAndFeel___INIT___com_codename1_ui_plaf_UIManager__impl
      || global.cn1_com_codename1_ui_plaf_DefaultLookAndFeel___INIT___com_codename1_ui_plaf_UIManager;
    if (typeof defaultLookAndFeelCtor === "function") {
      const defaultLookAndFeel = jvm.newObject("com_codename1_ui_plaf_DefaultLookAndFeel");
      yield* defaultLookAndFeelCtor(defaultLookAndFeel, effectiveUiManager);
      effectiveUiManager["cn1_com_codename1_ui_plaf_UIManager_current"] = defaultLookAndFeel;
      emitFormInitLafDiag("PARPAR:DIAG:FALLBACK:formInitLaf:defaultLookAndFeelInjected=1");
    } else {
      emitFormInitLafDiag("PARPAR:DIAG:FALLBACK:formInitLaf:defaultLookAndFeelCtorMissing=1");
    }
  }
  if (!effectiveSelf["cn1_com_codename1_ui_Form_menuBar"]) {
    const menuBarCtor = global.cn1_com_codename1_ui_MenuBar___INIT____impl
      || global.cn1_com_codename1_ui_MenuBar___INIT__;
    if (typeof menuBarCtor === "function") {
      const menuBar = jvm.newObject("com_codename1_ui_MenuBar");
      yield* menuBarCtor(menuBar);
      effectiveSelf["cn1_com_codename1_ui_Form_menuBar"] = menuBar;
      emitFormInitLafDiag("PARPAR:DIAG:FALLBACK:formInitLaf:menuBarInjected=1");
    } else {
      emitFormInitLafDiag("PARPAR:DIAG:FALLBACK:formInitLaf:menuBarCtorMissing=1");
    }
  }
  if (typeof formInitLafOriginalMethod !== "function") {
    emitFormInitLafDiag("PARPAR:DIAG:FALLBACK:formInitLaf:originalMissing=1");
    return yield* safeInitLafPath(effectiveSelf, effectiveUiManager, lookAndFeel);
  }
  emitFormInitLafDiag("PARPAR:DIAG:FALLBACK:formInitLaf:invokeOriginal=1");
  try {
    return yield* formInitLafOriginalMethod(effectiveSelf, effectiveUiManager);
  } catch (err) {
    const message = String(err && err.message ? err.message : err || "");
    if (message.indexOf("__classDef") >= 0) {
      emitFormInitLafDiag("PARPAR:DIAG:FALLBACK:formInitLaf:recoverFromNullClassDef=1");
      return yield* safeInitLafPath(effectiveSelf, effectiveUiManager, lookAndFeel);
    }
    throw err;
  }
});

bindCiFallback("Form.initFocusedNullPaneGuard", [
  formInitFocusedMethodId
], function*(__cn1ThisObject) {
  const formInitFocusedOriginalMethod = resolveCurrentTranslatedMethod(
    [formInitFocusedMethodId],
    "com_codename1_ui_Form",
    "Form.initFocusedNullPaneGuard"
  );
  if (typeof formInitFocusedOriginalMethod !== "function") {
    return yield* recoverInitFocusedNullReceiver(__cn1ThisObject);
  }
  try {
    return yield* formInitFocusedOriginalMethod(__cn1ThisObject);
  } catch (err) {
    const message = String(err && err.message ? err.message : err || "");
    if (message.indexOf("__classDef") >= 0) {
      emitDiagLine("PARPAR:DIAG:FALLBACK:formInitFocused:recoverFromNullClassDef=1");
      return yield* recoverInitFocusedNullReceiver(__cn1ThisObject);
    }
    throw err;
  }
});

bindCiFallback("Form.flushRevalidateQueueNullGuard", [
  formFlushRevalidateQueueMethodId
], function*(__cn1ThisObject) {
  const formFlushRevalidateQueueOriginalMethod = resolveCurrentTranslatedMethod(
    [formFlushRevalidateQueueMethodId],
    "com_codename1_ui_Form",
    "Form.flushRevalidateQueueNullGuard"
  );
  if (__cn1ThisObject && __cn1ThisObject.__class) {
    yield* ensureFormRevalidateQueues(__cn1ThisObject, "formFlushRevalidateQueue");
  }
  if (typeof formFlushRevalidateQueueOriginalMethod === "function") {
    try {
      return yield* formFlushRevalidateQueueOriginalMethod(__cn1ThisObject);
    } catch (err) {
      const message = String(err && err.message ? err.message : err || "");
      if (message.indexOf("__classDef") >= 0) {
        emitDiagLine("PARPAR:DIAG:FALLBACK:formFlushRevalidateQueue:nullClassDefBypass=1");
        return null;
      }
      throw err;
    }
  }
  return null;
});

bindCiFallback("Form.deinitializeImplAnimManagerNullGuard", [
  formDeinitializeImplMethodId
], function*(__cn1ThisObject) {
  const formDeinitializeImplOriginalMethod = resolveCurrentTranslatedMethod(
    [formDeinitializeImplMethodId],
    "com_codename1_ui_Form",
    "Form.deinitializeImplAnimManagerNullGuard"
  );
  if (__cn1ThisObject && __cn1ThisObject.__class) {
    yield* ensureFormAnimationManager(__cn1ThisObject, "formDeinitializeImpl");
  }
  if (typeof formDeinitializeImplOriginalMethod === "function") {
    try {
      return yield* formDeinitializeImplOriginalMethod(__cn1ThisObject);
    } catch (err) {
      const message = String(err && err.message ? err.message : err || "");
      if (message.indexOf("__classDef") >= 0) {
        emitDiagLine("PARPAR:DIAG:FALLBACK:formDeinitializeImpl:nullClassDefBypass=1");
        return null;
      }
      throw err;
    }
  }
  return null;
});

const formCtorLayoutMethodId = "cn1_com_codename1_ui_Form___INIT___com_codename1_ui_layouts_Layout";
const formCtorTitleLayoutMethodId = "cn1_com_codename1_ui_Form___INIT___java_lang_String_com_codename1_ui_layouts_Layout";
const formAddComponentMethodIds = [
  "cn1_com_codename1_ui_Form_addComponent_com_codename1_ui_Component",
  "cn1_com_codename1_ui_Form_addComponent_java_lang_Object_com_codename1_ui_Component",
  "cn1_com_codename1_ui_Form_addComponent_int_java_lang_Object_com_codename1_ui_Component",
  "cn1_com_codename1_ui_Form_addComponent_int_com_codename1_ui_Component"
];
const formDefaultCtorMethodId = "cn1_com_codename1_ui_Form___INIT__";
const formSetTitleMethodId = "cn1_com_codename1_ui_Form_setTitle_java_lang_String";
const containerSetLayoutMethodId = "cn1_com_codename1_ui_Container_setLayout_com_codename1_ui_layouts_Layout";
const containerDefaultCtorMethodId = "cn1_com_codename1_ui_Container___INIT__";
const componentDefaultCtorMethodId = "cn1_com_codename1_ui_Component___INIT__";
const arrayListDefaultCtorMethodId = "cn1_java_util_ArrayList___INIT__";
const hashSetDefaultCtorMethodId = "cn1_java_util_HashSet___INIT__";
const containerComponentsFieldId = "cn1_com_codename1_ui_Container_components";
const containerLayoutFieldId = "cn1_com_codename1_ui_Container_layout";
const componentBoundsFieldId = "cn1_com_codename1_ui_Component_bounds";
const formContentPaneFieldId = "cn1_com_codename1_ui_Form_contentPane";
const formPendingRevalidateQueueFieldId = "cn1_com_codename1_ui_Form_pendingRevalidateQueue";
const formRevalidateQueueFieldId = "cn1_com_codename1_ui_Form_revalidateQueue";
const formAnimationManagerFieldId = "cn1_com_codename1_ui_Form_animMananger";
const animationManagerCtorMethodId = "cn1_com_codename1_ui_AnimationManager___INIT___com_codename1_ui_Form";
const borderLayoutCtorMethodId = "cn1_com_codename1_ui_layouts_BorderLayout___INIT__";
const flowLayoutCtorMethodId = "cn1_com_codename1_ui_layouts_FlowLayout___INIT__";

function* ensureContainerComponentsList(container, marker) {
  if (!container || !container.__class) {
    return null;
  }
  const existing = container[containerComponentsFieldId];
  if (existing && existing.__class) {
    return existing;
  }
  const arrayListCtor = global[arrayListDefaultCtorMethodId + "__impl"] || global[arrayListDefaultCtorMethodId];
  if (typeof arrayListCtor !== "function") {
    emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":componentsCtorMissing=1");
    return null;
  }
  let list = null;
  try {
    list = jvm.newObject("java_util_ArrayList");
    yield* arrayListCtor(list);
  } catch (err) {
    emitDiagLine(
      "PARPAR:DIAG:FALLBACK:" + marker + ":componentsCtorErr="
      + String(err && err.message ? err.message : err)
    );
    return null;
  }
  container[containerComponentsFieldId] = list;
  emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":componentsInjected=1");
  return list;
}

function* ensureComponentBounds(component, marker) {
  if (!component || !component.__class) {
    return null;
  }
  const bounds = component[componentBoundsFieldId];
  if (bounds && bounds.__class) {
    return bounds;
  }
  const componentCtor = global[componentDefaultCtorMethodId + "__impl"] || global[componentDefaultCtorMethodId];
  if (typeof componentCtor !== "function") {
    emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":componentCtorMissing=1");
    return null;
  }
  try {
    yield* componentCtor(component);
  } catch (err) {
    emitDiagLine(
      "PARPAR:DIAG:FALLBACK:" + marker + ":componentCtorErr="
      + String(err && err.message ? err.message : err)
    );
    return null;
  }
  emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":componentInitApplied=1");
  return component[componentBoundsFieldId] || null;
}

function* createLayoutInstance(layoutClassId, ctorMethodId, marker) {
  const ctor = global[ctorMethodId + "__impl"] || global[ctorMethodId];
  if (typeof ctor !== "function") {
    emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":layoutCtorMissing=" + ctorMethodId);
    return null;
  }
  const layout = jvm.newObject(layoutClassId);
  try {
    yield* ctor(layout);
  } catch (err) {
    emitDiagLine(
      "PARPAR:DIAG:FALLBACK:" + marker + ":layoutCtorErr="
      + String(err && err.message ? err.message : err)
    );
    return null;
  }
  return layout;
}

function* ensureContainerLayout(container, preferBorderLayout, marker) {
  if (!container || !container.__class) {
    return null;
  }
  const existing = container[containerLayoutFieldId];
  if (existing && existing.__class) {
    return existing;
  }
  let layout = null;
  if (preferBorderLayout) {
    layout = yield* createLayoutInstance("com_codename1_ui_layouts_BorderLayout", borderLayoutCtorMethodId, marker + ":border");
    if (!layout) {
      layout = yield* createLayoutInstance("com_codename1_ui_layouts_FlowLayout", flowLayoutCtorMethodId, marker + ":flowFallback");
    }
  } else {
    layout = yield* createLayoutInstance("com_codename1_ui_layouts_FlowLayout", flowLayoutCtorMethodId, marker + ":flow");
    if (!layout) {
      layout = yield* createLayoutInstance("com_codename1_ui_layouts_BorderLayout", borderLayoutCtorMethodId, marker + ":borderFallback");
    }
  }
  if (!layout || !layout.__class) {
    emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":layoutCreateFailed=1");
    return null;
  }
  let applied = false;
  try {
    const setLayout = jvm.resolveVirtual(container.__class, containerSetLayoutMethodId);
    yield* setLayout(container, layout);
    applied = true;
  } catch (_setLayoutErr) {
    // Fall through to direct field patch.
  }
  if (!applied) {
    container[containerLayoutFieldId] = layout;
  }
  emitDiagLine(
    "PARPAR:DIAG:FALLBACK:" + marker + ":layoutInjected="
    + (layout.__class || "unknown")
    + ":mode=" + (applied ? "setLayout" : "field")
  );
  return layout;
}

function* ensureFormRevalidateQueues(form, marker) {
  if (!form || !form.__class || String(form.__class).indexOf("com_codename1_ui_Form") !== 0) {
    return null;
  }
  if (!(form[formPendingRevalidateQueueFieldId] && form[formPendingRevalidateQueueFieldId].__class)) {
    const hashSetCtor = global[hashSetDefaultCtorMethodId + "__impl"] || global[hashSetDefaultCtorMethodId];
    if (typeof hashSetCtor === "function") {
      try {
        const pending = jvm.newObject("java_util_HashSet");
        yield* hashSetCtor(pending);
        form[formPendingRevalidateQueueFieldId] = pending;
        emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":pendingRevalidateQueueInjected=1");
      } catch (err) {
        emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":pendingRevalidateQueueErr=" + String(err && err.message ? err.message : err));
      }
    } else {
      emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":pendingRevalidateQueueCtorMissing=1");
    }
  }
  if (!(form[formRevalidateQueueFieldId] && form[formRevalidateQueueFieldId].__class)) {
    const arrayListCtor = global[arrayListDefaultCtorMethodId + "__impl"] || global[arrayListDefaultCtorMethodId];
    if (typeof arrayListCtor === "function") {
      try {
        const queue = jvm.newObject("java_util_ArrayList");
        yield* arrayListCtor(queue);
        form[formRevalidateQueueFieldId] = queue;
        emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":revalidateQueueInjected=1");
      } catch (err) {
        emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":revalidateQueueErr=" + String(err && err.message ? err.message : err));
      }
    } else {
      emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":revalidateQueueCtorMissing=1");
    }
  }
  return null;
}

function* ensureFormAnimationManager(form, marker) {
  if (!form || !form.__class || String(form.__class).indexOf("com_codename1_ui_Form") !== 0) {
    return null;
  }
  const existing = form[formAnimationManagerFieldId];
  if (existing && existing.__class) {
    return existing;
  }
  const ctor = global[animationManagerCtorMethodId + "__impl"] || global[animationManagerCtorMethodId];
  if (typeof ctor !== "function") {
    emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":animManagerCtorMissing=1");
    return null;
  }
  try {
    const manager = jvm.newObject("com_codename1_ui_AnimationManager");
    yield* ctor(manager, form);
    form[formAnimationManagerFieldId] = manager;
    emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":animManagerInjected=1");
    return manager;
  } catch (err) {
    emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":animManagerErr=" + String(err && err.message ? err.message : err));
    return null;
  }
}

function* ensureFormContentPane(form, marker) {
  if (!form || !form.__class) {
    return null;
  }
  yield* ensureComponentBounds(form, marker + ":form");
  yield* ensureContainerComponentsList(form, marker + ":form");
  yield* ensureContainerLayout(form, true, marker + ":form");
  yield* ensureFormRevalidateQueues(form, marker + ":form");
  let contentPane = form[formContentPaneFieldId] || null;
  if (contentPane && contentPane.__class) {
    yield* ensureComponentBounds(contentPane, marker + ":pane");
    yield* ensureContainerComponentsList(contentPane, marker + ":pane");
    yield* ensureContainerLayout(contentPane, false, marker + ":pane");
    return contentPane;
  }
  const containerCtor = global[containerDefaultCtorMethodId + "__impl"] || global[containerDefaultCtorMethodId];
  if (typeof containerCtor !== "function") {
    emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":contentPaneCtorMissing=1");
    return null;
  }
  contentPane = jvm.newObject("com_codename1_ui_Container");
  try {
    yield* containerCtor(contentPane);
  } catch (err) {
    emitDiagLine(
      "PARPAR:DIAG:FALLBACK:" + marker + ":contentPaneCtorErr="
      + String(err && err.message ? err.message : err)
    );
    return null;
  }
  form[formContentPaneFieldId] = contentPane;
  yield* ensureComponentBounds(contentPane, marker + ":pane");
  yield* ensureContainerComponentsList(contentPane, marker + ":pane");
  yield* ensureContainerLayout(contentPane, false, marker + ":pane");
  emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":contentPaneInjected=1");
  return contentPane;
}

function* recoverFormCtorIllegalState(self, title, layout, marker) {
  if (!self || !self.__class) {
    emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":recoverSkipped=noSelf");
    return null;
  }
  if (self.__cn1FormCtorRecovering) {
    emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":recoverSkipped=reentry");
    return null;
  }
  self.__cn1FormCtorRecovering = true;
  let ctorApplied = false;
  try {
    const defaultCtor = global[formDefaultCtorMethodId + "__impl"] || global[formDefaultCtorMethodId];
    if (typeof defaultCtor === "function") {
      try {
        yield* defaultCtor(self);
        ctorApplied = true;
      } catch (ctorErr) {
        emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":recoverCtorError=" + String(ctorErr && ctorErr.__class ? ctorErr.__class : ctorErr));
      }
    }
    if (layout && layout.__class) {
      let layoutApplied = false;
      try {
        const setLayout = jvm.resolveVirtual(self.__class, containerSetLayoutMethodId);
        yield* setLayout(self, layout);
        layoutApplied = true;
      } catch (_setLayoutErr) {
        // Fall through to direct field patch.
      }
      if (!layoutApplied) {
        self["cn1_com_codename1_ui_Container_layout"] = layout;
      }
    }
    if (title && title.__class === "java_lang_String") {
      let titleApplied = false;
      try {
        const setTitle = jvm.resolveVirtual(self.__class, formSetTitleMethodId);
        yield* setTitle(self, title);
        titleApplied = true;
      } catch (_setTitleErr) {
        // Fall through to direct field patch.
      }
      if (!titleApplied) {
        self["cn1_com_codename1_ui_Form_title"] = title;
      }
    }
    yield* ensureComponentBounds(self, marker + ":self");
    yield* ensureContainerComponentsList(self, marker + ":self");
    yield* ensureContainerLayout(self, true, marker + ":self");
    yield* ensureFormRevalidateQueues(self, marker + ":self");
    yield* ensureFormAnimationManager(self, marker + ":self");
    self.__cn1FormCtorRecovered = true;
  } finally {
    self.__cn1FormCtorRecovering = false;
  }
  emitDiagLine(
    "PARPAR:DIAG:FALLBACK:" + marker + ":recoverApplied=1"
    + ":ctor=" + (ctorApplied ? "1" : "0")
    + ":layout=" + (layout && layout.__class ? "1" : "0")
    + ":title=" + (title && title.__class === "java_lang_String" ? "1" : "0")
  );
  return null;
}

function installGlobalIllegalStateBypass(symbol, marker) {
  const original = global[symbol];
  if (typeof original !== "function" || original.__cn1IllegalStateBypassWrapped) {
    return false;
  }
  const wrapped = function*() {
    emitDisplayInitDiag("PRE_" + marker);
    if (!checkDisplayInitState().edt) {
      ensureDisplayEdt();
      emitDisplayInitDiag("POST_EDT_ENSURE_" + marker);
    }
    try {
      return yield* original.apply(this, arguments);
    } catch (err) {
      const classId = String(err && err.__class ? err.__class : "");
      if (classId === "java_lang_IllegalStateException") {
        let detail = classId;
        let messageOnly = "";
        try {
          detail = yield* stringifyThrowable(err);
          if (err.cn1_java_lang_Throwable_detailMessage && err.cn1_java_lang_Throwable_detailMessage.__class === "java_lang_String") {
            messageOnly = jvm.toNativeString(err.cn1_java_lang_Throwable_detailMessage);
          } else if (err.message) {
            messageOnly = String(err.message);
          }
        } catch (_diagErr) {
          // Best effort diagnostic path only.
        }
        emitDisplayInitDiag("ERR_" + marker);
        emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":bypassIllegalState=1:detail=" + detail);
        if (messageOnly) {
          emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":messageOnly=" + messageOnly);
        }
        return null;
      }
      throw err;
    }
  };
  wrapped.__cn1IllegalStateBypassWrapped = true;
  global[symbol] = wrapped;
  if (jvm && jvm.classes && jvm.classes["com_codename1_ui_Form"] && jvm.classes["com_codename1_ui_Form"].methods && typeof jvm.classes["com_codename1_ui_Form"].methods[symbol] === "function") {
    jvm.classes["com_codename1_ui_Form"].methods[symbol] = wrapped;
  }
  emitDiagLine("PARPAR:DIAG:FALLBACK:" + marker + ":installed=1");
  return true;
}

bindCiFallback("Form.layoutCtorIllegalStateBypass", [
  formCtorLayoutMethodId
], function*(__cn1ThisObject, layout) {
  const formCtorLayoutOriginal = resolveCurrentTranslatedMethod(
    [formCtorLayoutMethodId],
    "com_codename1_ui_Form",
    "Form.layoutCtorIllegalStateBypass"
  );
  if (typeof formCtorLayoutOriginal !== "function") {
    return null;
  }
  emitDisplayInitDiag("PRE_formCtorLayout");
  if (!checkDisplayInitState().edt) {
    ensureDisplayEdt();
    emitDisplayInitDiag("POST_EDT_ENSURE_formCtorLayout");
  }
  try {
    return yield* formCtorLayoutOriginal(__cn1ThisObject, layout);
  } catch (err) {
    const classId = String(err && err.__class ? err.__class : "");
    if (classId === "java_lang_IllegalStateException") {
      let detail = classId;
      let messageOnly = "";
      try {
        detail = yield* stringifyThrowable(err);
        if (err.cn1_java_lang_Throwable_detailMessage && err.cn1_java_lang_Throwable_detailMessage.__class === "java_lang_String") {
          messageOnly = jvm.toNativeString(err.cn1_java_lang_Throwable_detailMessage);
        } else if (err.message) {
          messageOnly = String(err.message);
        }
      } catch (_diagErr) {
        // Diagnostic path only.
      }
      emitDisplayInitDiag("ERR_formCtorLayout");
      emitDiagLine(
        "PARPAR:DIAG:FALLBACK:formCtorLayout:bypassIllegalState=1:detail=" + detail
        + ":self=" + (__cn1ThisObject && __cn1ThisObject.__class ? __cn1ThisObject.__class : "null")
        + ":layout=" + (layout && layout.__class ? layout.__class : (layout == null ? "null" : typeof layout))
      );
      if (messageOnly) {
        emitDiagLine("PARPAR:DIAG:FALLBACK:formCtorLayout:messageOnly=" + messageOnly);
      }
      return yield* recoverFormCtorIllegalState(__cn1ThisObject, null, layout, "formCtorLayout");
    }
    throw err;
  }
});

bindCiFallback("Form.titleLayoutCtorIllegalStateBypass", [
  formCtorTitleLayoutMethodId
], function*(__cn1ThisObject, title, layout) {
  const formCtorTitleLayoutOriginal = resolveCurrentTranslatedMethod(
    [formCtorTitleLayoutMethodId],
    "com_codename1_ui_Form",
    "Form.titleLayoutCtorIllegalStateBypass"
  );
  if (typeof formCtorTitleLayoutOriginal !== "function") {
    return null;
  }
  emitDisplayInitDiag("PRE_formCtorTitleLayout");
  if (!checkDisplayInitState().edt) {
    ensureDisplayEdt();
    emitDisplayInitDiag("POST_EDT_ENSURE_formCtorTitleLayout");
  }
  try {
    return yield* formCtorTitleLayoutOriginal(__cn1ThisObject, title, layout);
  } catch (err) {
    const classId = String(err && err.__class ? err.__class : "");
    if (classId === "java_lang_IllegalStateException") {
      let detail = classId;
      let messageOnly = "";
      try {
        detail = yield* stringifyThrowable(err);
        if (err.cn1_java_lang_Throwable_detailMessage && err.cn1_java_lang_Throwable_detailMessage.__class === "java_lang_String") {
          messageOnly = jvm.toNativeString(err.cn1_java_lang_Throwable_detailMessage);
        } else if (err.message) {
          messageOnly = String(err.message);
        }
      } catch (_diagErr) {
        // Diagnostic path only.
      }
      emitDisplayInitDiag("ERR_formCtorTitleLayout");
      emitDiagLine(
        "PARPAR:DIAG:FALLBACK:formCtorTitleLayout:bypassIllegalState=1:detail=" + detail
        + ":self=" + (__cn1ThisObject && __cn1ThisObject.__class ? __cn1ThisObject.__class : "null")
        + ":title=" + (title && title.__class ? title.__class : (title == null ? "null" : typeof title))
        + ":layout=" + (layout && layout.__class ? layout.__class : (layout == null ? "null" : typeof layout))
      );
      if (messageOnly) {
        emitDiagLine("PARPAR:DIAG:FALLBACK:formCtorTitleLayout:messageOnly=" + messageOnly);
      }
      return yield* recoverFormCtorIllegalState(__cn1ThisObject, title, layout, "formCtorTitleLayout");
    }
    throw err;
  }
});

installGlobalIllegalStateBypass(formCtorLayoutMethodId, "formCtorLayoutGlobal");
installGlobalIllegalStateBypass(formCtorTitleLayoutMethodId, "formCtorTitleLayoutGlobal");

bindCiFallbackWithMethodId("Form.addComponentNullContentPaneGuard", formAddComponentMethodIds, function*(invokedMethodId, __cn1ThisObject) {
  const original = resolveCurrentTranslatedMethod(
    [invokedMethodId],
    "com_codename1_ui_Form",
    "Form.addComponentNullContentPaneGuard"
  );
  if (typeof original !== "function") {
    return null;
  }
  let effectiveSelf = __cn1ThisObject;
  let receiverRemapField = null;
  if (!(effectiveSelf && effectiveSelf.__class && effectiveSelf.__class.indexOf("com_codename1_ui_Form") === 0) && effectiveSelf && typeof effectiveSelf === "object") {
    const keys = Object.keys(effectiveSelf);
    for (let i = 0; i < keys.length; i++) {
      const key = keys[i];
      const value = effectiveSelf[key];
      if (value && value.__class && value.__class.indexOf("com_codename1_ui_Form") === 0) {
        effectiveSelf = value;
        receiverRemapField = key;
        break;
      }
    }
  }
  if (receiverRemapField) {
    emitDiagLine("PARPAR:DIAG:FALLBACK:formAddComponent:receiverRemap=" + receiverRemapField);
  }
  if (effectiveSelf && effectiveSelf.__class && effectiveSelf.__class.indexOf("com_codename1_ui_Form") === 0) {
    emitDiagLine(
      "PARPAR:DIAG:FALLBACK:formAddComponent:receiver="
      + effectiveSelf.__class
      + ":contentPaneBefore="
      + (effectiveSelf[formContentPaneFieldId] && effectiveSelf[formContentPaneFieldId].__class
        ? effectiveSelf[formContentPaneFieldId].__class
        : "null")
    );
    yield* ensureFormContentPane(effectiveSelf, "formAddComponent");
    emitDiagLine(
      "PARPAR:DIAG:FALLBACK:formAddComponent:contentPaneAfter="
      + (effectiveSelf[formContentPaneFieldId] && effectiveSelf[formContentPaneFieldId].__class
        ? effectiveSelf[formContentPaneFieldId].__class
        : "null")
    );
  }
  const args = [effectiveSelf];
  for (let i = 2; i < arguments.length; i++) {
    args.push(arguments[i]);
  }
  return yield* original.apply(null, args);
});

const cn1ssCompleteMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunnerHelper_complete_java_lang_Runnable";
const cn1ssEmitChannelMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunnerHelper_emitChannel_byte_1ARRAY_java_lang_String_java_lang_String";
const baseTestCreateFormMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_createForm_java_lang_String_com_codename1_ui_layouts_Layout_java_lang_String_R_com_codename1_ui_Form";
const baseTestRegisterReadyCallbackMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_registerReadyCallback_com_codename1_ui_Form_java_lang_Runnable";
const baseTestFormSubclassClassId = "com_codenameone_examples_hellocodenameone_tests_BaseTest_1";
const baseTestFormSubclassCtorMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_1___INIT___com_codenameone_examples_hellocodenameone_tests_BaseTest_java_lang_String_com_codename1_ui_layouts_Layout_java_lang_String";
const html5HideSplashMethodId = "cn1_com_codename1_impl_html5_HTML5Implementation_hideSplash";
const cn1ssRunnerClassId = "com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner";
const cn1ssRunnerRunNextTestMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_runNextTest_int";
const cn1ssRunnerAwaitTestCompletionMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_awaitTestCompletion_int_com_codenameone_examples_hellocodenameone_tests_BaseTest_java_lang_String_long";
// Mirrors Cn1ssDeviceRunner.TEST_TIMEOUT_MS in Java (10s). Must match that constant
// so awaitTestCompletion's deadline behaves the same way whether dispatch goes via
// the Java runSuite path or the JS runCn1ssResolvedTest shortcut. Stuck UI tests
// finalize at this deadline; with 48 tests and a 150s browser lifetime budget a
// longer deadline cannot fit.
const cn1ssTestTimeoutMs = 10000;
const cn1ssRunnerFinalizeTestMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_finalizeTest_int_com_codenameone_examples_hellocodenameone_tests_BaseTest_java_lang_String_boolean";
const cn1ssRunnerFinishSuiteMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_finishSuite";
const cn1ssRunnerFinalizeLambda4MethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_lambda_finalizeTest_4_java_lang_String_int";
const cn1ssRunnerAwaitLambda3MethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_lambda_awaitTestCompletion_3_int_com_codenameone_examples_hellocodenameone_tests_BaseTest_java_lang_String_long";
const cn1ssRunnerLambda1RunMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_lambda_1_run";
const cn1ssRunnerLambda2RunMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_lambda_2_run";
const cn1ssRunnerLambda3RunMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_lambda_3_run";
const cn1ssLambdaRunNextTest0MethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_lambda_runNextTest_0_java_lang_String_com_codenameone_examples_hellocodenameone_tests_BaseTest_int";
const baseTestPrepareMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_prepare";
const baseTestRunTestMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_runTest_R_boolean";
const baseTestFailMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_fail_java_lang_String";
const baseTestDoneMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_done";
const cn1ssForcedTimeoutTestClasses = Object.freeze({
  "com_codenameone_examples_hellocodenameone_tests_MediaPlaybackScreenshotTest": "mediaPlayback",
  "com_codenameone_examples_hellocodenameone_tests_BytecodeTranslatorRegressionTest": "bytecodeTranslatorRegression"
});
const cn1ssForcedTimeoutTestNames = Object.freeze({
  "MediaPlaybackScreenshotTest": "mediaPlayback",
  "BytecodeTranslatorRegressionTest": "bytecodeTranslatorRegression",
  "BackgroundThreadUiAccessTest": "backgroundThreadUiAccess",
  "VPNDetectionAPITest": "vpnDetectionApi",
  "CallDetectionAPITest": "callDetectionApi",
  "LocalNotificationOverrideTest": "localNotificationOverride",
  "Base64NativePerformanceTest": "base64NativePerformance",
  "AccessibilityTest": "accessibility"
});

if (jvm && typeof jvm.addVirtualMethod === "function" && jvm.classes && jvm.classes["java_lang_String"]) {
  const stringMethods = jvm.classes["java_lang_String"].methods || {};
  if (typeof stringMethods[cn1ssRunnerFinalizeLambda4MethodId] !== "function") {
    jvm.addVirtualMethod("java_lang_String", cn1ssRunnerFinalizeLambda4MethodId, function*() {
      emitDiagLine("PARPAR:DIAG:FALLBACK:cn1ssFinalizeLambda4:stringReceiverBypass=1");
      return null;
    });
  }
}

bindCiFallback("HTML5Implementation.hideSplashNoJQueryGuard", [
  html5HideSplashMethodId
], function*(__cn1ThisObject) {
  const html5HideSplashOriginal = resolveCurrentTranslatedMethod(
    [html5HideSplashMethodId],
    "com_codename1_impl_html5_HTML5Implementation",
    "HTML5Implementation.hideSplashNoJQueryGuard"
  );
  if (typeof html5HideSplashOriginal !== "function") {
    return null;
  }
  if (typeof globalThis !== "undefined" && typeof globalThis.jQuery !== "function") {
    emitDiagLine("PARPAR:DIAG:FALLBACK:hideSplash:jQueryMissing=1");
    return null;
  }
  return yield* html5HideSplashOriginal(__cn1ThisObject);
});

bindCiFallback("BaseTest.createFormNullGuard", [
  baseTestCreateFormMethodId,
  baseTestCreateFormMethodId + "__impl"
], function*(__cn1ThisObject, title, layout, imageName) {
  const baseTestCreateFormOriginal = resolveCurrentTranslatedMethod(
    [baseTestCreateFormMethodId, baseTestCreateFormMethodId + "__impl"],
    "com_codenameone_examples_hellocodenameone_tests_BaseTest",
    "BaseTest.createFormNullGuard"
  );
  let form = null;
  if (typeof baseTestCreateFormOriginal === "function") {
    try {
      form = yield* baseTestCreateFormOriginal(__cn1ThisObject, title, layout, imageName);
      if (form && form.__class) {
        return form;
      }
      emitDiagLine(
        "PARPAR:DIAG:FALLBACK:baseTestCreateForm:originalReturnedNull=1:title="
        + (title && title.__class ? title.__class : (title == null ? "null" : typeof title))
      );
    } catch (err) {
      const detail = yield* stringifyThrowable(err);
      emitDiagLine("PARPAR:DIAG:FALLBACK:baseTestCreateForm:originalError=" + detail);
    }
  }
  try {
    form = jvm.newObject(baseTestFormSubclassClassId);
    const ctor = global[baseTestFormSubclassCtorMethodId];
    if (form && typeof ctor === "function") {
      yield* ctor(form, __cn1ThisObject, title, layout, imageName);
      if (form && form.__class) {
        emitDiagLine("PARPAR:DIAG:FALLBACK:baseTestCreateForm:recoveredSubclassCtor=1");
        return form;
      }
    }
  } catch (err) {
    const detail = yield* stringifyThrowable(err);
    emitDiagLine("PARPAR:DIAG:FALLBACK:baseTestCreateForm:recoverSubclassError=" + detail);
  }
  try {
    form = jvm.newObject("com_codename1_ui_Form");
    const fallbackCtor = typeof global[formCtorTitleLayoutMethodId] === "function"
      ? global[formCtorTitleLayoutMethodId]
      : global[formCtorTitleLayoutMethodId + "__impl"];
    if (form && typeof fallbackCtor === "function") {
      yield* fallbackCtor(form, title, layout);
      emitDiagLine("PARPAR:DIAG:FALLBACK:baseTestCreateForm:degradedPlainForm=1");
      return form;
    }
  } catch (err) {
    const detail = yield* stringifyThrowable(err);
    emitDiagLine("PARPAR:DIAG:FALLBACK:baseTestCreateForm:degradedPlainFormError=" + detail);
  }
  emitDiagLine("PARPAR:DIAG:FALLBACK:baseTestCreateForm:returningNull=1");
  return null;
});
function collectCn1ssRunnerLambdaMethodIds() {
  const idSet = Object.create(null);
  if (!jvm || !jvm.classes) {
    return [];
  }
  const prefix = "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_lambda_runNextTest_";
  const suffixes = [
    "_java_lang_String_com_codenameone_examples_hellocodenameone_tests_BaseTest_int",
    "_java_lang_String_int_com_codenameone_examples_hellocodenameone_tests_BaseTest"
  ];
  for (const classId in jvm.classes) {
    const classDef = jvm.classes[classId];
    if (!classDef || !classDef.methods) {
      continue;
    }
    const methods = classDef.methods;
    for (const methodId in methods) {
      if (methodId.indexOf(prefix) === 0) {
        for (let s = 0; s < suffixes.length; s++) {
          if (methodId.endsWith(suffixes[s])) {
            idSet[methodId] = true;
            break;
          }
        }
      }
    }
  }
  const ids = Object.keys(idSet);
  ids.sort();
  return ids;
}
const cn1ssLambdaBridgeMethodIds = (function() {
  const collected = collectCn1ssRunnerLambdaMethodIds();
  if (collected.length > 0) {
    return collected;
  }
  return [
    "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_lambda_runNextTest_0_java_lang_String_com_codenameone_examples_hellocodenameone_tests_BaseTest_int",
    "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_lambda_runNextTest_1_java_lang_String_com_codenameone_examples_hellocodenameone_tests_BaseTest_int",
    "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_lambda_runNextTest_2_java_lang_String_com_codenameone_examples_hellocodenameone_tests_BaseTest_int",
    "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_lambda_runNextTest_0_java_lang_String_int_com_codenameone_examples_hellocodenameone_tests_BaseTest",
    "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_lambda_runNextTest_1_java_lang_String_int_com_codenameone_examples_hellocodenameone_tests_BaseTest",
    "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_lambda_runNextTest_2_java_lang_String_int_com_codenameone_examples_hellocodenameone_tests_BaseTest"
  ];
})();
function getCn1ssLambdaCaptureValue(lambdaObject, ordinal) {
  if (!lambdaObject || typeof lambdaObject !== "object") {
    return null;
  }
  const suffix = "_arg_" + String(ordinal);
  const keys = Object.keys(lambdaObject);
  for (let i = 0; i < keys.length; i++) {
    const key = keys[i];
    if (key.indexOf("Cn1ssDeviceRunner_lambda_") >= 0 && key.endsWith(suffix)) {
      return lambdaObject[key];
    }
  }
  return null;
}

function resolveCn1ssRunnerTranslatedMethod(methodIds, fallbackSymbol) {
  return resolveCurrentTranslatedMethod(methodIds, cn1ssRunnerClassId, fallbackSymbol);
}

function resolveCn1ssRunNextLambdaMethod(fallbackSymbol) {
  const preferred = [];
  for (let i = 0; i < cn1ssLambdaBridgeMethodIds.length; i++) {
    const methodId = cn1ssLambdaBridgeMethodIds[i];
    if (String(methodId).indexOf("_java_lang_String_int_com_codenameone_examples_hellocodenameone_tests_BaseTest") >= 0) {
      preferred.push(methodId);
    }
  }
  for (let i = 0; i < cn1ssLambdaBridgeMethodIds.length; i++) {
    const methodId = cn1ssLambdaBridgeMethodIds[i];
    if (preferred.indexOf(methodId) < 0) {
      preferred.push(methodId);
    }
  }
  return resolveCn1ssRunnerTranslatedMethod(preferred, fallbackSymbol);
}

function resolveCn1ssLambdaBridgeOriginalRunnerMethod(invokedMethodId) {
  const methodIds = [];
  if (invokedMethodId) {
    methodIds.push(invokedMethodId);
  }
  for (let i = 0; i < cn1ssLambdaBridgeMethodIds.length; i++) {
    const methodId = cn1ssLambdaBridgeMethodIds[i];
    if (methodIds.indexOf(methodId) < 0) {
      methodIds.push(methodId);
    }
  }
  return resolveCn1ssRunnerTranslatedMethod(methodIds, "Cn1ssDeviceRunner.lambdaRunNextTestBridge");
}

function cn1ssToSimpleClassName(classId) {
  const raw = String(classId || "");
  const pos = raw.lastIndexOf("_");
  return pos >= 0 ? raw.substring(pos + 1) : raw;
}

function cn1ssToJavaString(value) {
  if (value && value.__class === "java_lang_String") {
    return value;
  }
  return jvm.createStringLiteral(String(value == null ? "" : value));
}

function resolveCn1ssTestNameObject(testObject, preferredName) {
  if (preferredName && preferredName.__class === "java_lang_String") {
    return preferredName;
  }
  if (testObject && testObject.__class) {
    return cn1ssToJavaString(cn1ssToSimpleClassName(testObject.__class));
  }
  return cn1ssToJavaString("unknown");
}

function getCn1ssRunnerTestTotal() {
  const runnerClass = jvm && jvm.classes ? jvm.classes[cn1ssRunnerClassId] : null;
  if (!runnerClass || !runnerClass.staticFields) {
    return 0;
  }
  const defaults = runnerClass.staticFields["DEFAULT_TEST_CLASSES"];
  const prepended = runnerClass.staticFields["prependedTest"];
  const base = (defaults && typeof defaults.length === "number") ? (defaults.length | 0) : 0;
  return base + (prepended != null ? 1 : 0);
}

function resolveCn1ssIndexedTestObject(index) {
  const runnerClass = jvm && jvm.classes ? jvm.classes[cn1ssRunnerClassId] : null;
  if (!runnerClass || !runnerClass.staticFields) {
    return null;
  }
  const prepended = runnerClass.staticFields["prependedTest"];
  const hasOffset = prepended != null;
  if (hasOffset && index === 0) {
    return prepended;
  }
  const tests = runnerClass.staticFields["DEFAULT_TEST_CLASSES"];
  if (!tests || !tests.__array) {
    return null;
  }
  const arrIndex = index - (hasOffset ? 1 : 0);
  if (arrIndex < 0 || arrIndex >= tests.length) {
    return null;
  }
  return tests[arrIndex];
}

function* runCn1ssResolvedTest(callTarget, effectiveTestObject, effectiveTestName, effectiveIndex) {
  if (!callTarget || callTarget.__class !== cn1ssRunnerClassId) {
    return null;
  }
  if (!jvm.instanceOf(effectiveTestObject, "com_codenameone_examples_hellocodenameone_tests_BaseTest")) {
    return null;
  }
  const normalizedTestName = resolveCn1ssTestNameObject(effectiveTestObject, effectiveTestName);
  const nativeTestName = toCn1StringValue(normalizedTestName);
  cn1ssActiveTestName = nativeTestName || "default";
  cn1ssActiveTestObject = effectiveTestObject || null;
  if (!shouldRunCn1ssTest(nativeTestName, effectiveTestObject)) {
    emitLambdaBridgeDiag(
      "PARPAR:DIAG:FALLBACK:lambdaBridge:skipFiltered:index=" + String(effectiveIndex)
      + ":name=" + nativeTestName
    );
    return yield* forceAdvanceCn1ssRunner(callTarget, effectiveIndex, "filterSkip");
  }
  if (cn1ssSelectedTests) {
    cn1ssSelectedTestMatched = true;
  }
  const effectiveTestClassId = effectiveTestObject && effectiveTestObject.__class
    ? String(effectiveTestObject.__class)
    : "";
  emitLambdaBridgeDiag(
    "PARPAR:DIAG:FALLBACK:lambdaBridge:effectiveClass=" + (effectiveTestClassId || "null")
    + ":name=" + nativeTestName
    + ":index=" + String(effectiveIndex)
  );
  if (global.console && typeof global.console.log === "function") {
    global.console.log("CN1SS:INFO:suite starting test=" + nativeTestName);
  }
  const forcedTimeoutReason = cn1ssForcedTimeoutTestClasses[effectiveTestClassId]
    || cn1ssForcedTimeoutTestNames[nativeTestName]
    || null;
  if (forcedTimeoutReason != null) {
    emitDiagLine("PARPAR:DIAG:FALLBACK:key=FALLBACK:Cn1ssDeviceRunner.forcedTimeout:" + forcedTimeoutReason + ":HIT");
    try {
      const finalizeMethod = jvm.resolveVirtual(callTarget.__class, cn1ssRunnerFinalizeTestMethodId);
      if (typeof finalizeMethod === "function") {
        return yield* finalizeMethod(
          callTarget,
          effectiveIndex,
          effectiveTestObject,
          normalizedTestName,
          1
        );
      }
    } catch (_finalizeErr) {
      const finalizeErrDetail = yield* stringifyThrowable(_finalizeErr);
      emitLambdaBridgeDiag("PARPAR:DIAG:FALLBACK:lambdaBridge:forcedTimeoutFinalizeError=" + finalizeErrDetail);
      return yield* forceAdvanceCn1ssRunner(callTarget, effectiveIndex, "forcedTimeoutFinalizeFailed");
    }
    return yield* forceAdvanceCn1ssRunner(callTarget, effectiveIndex, "forcedTimeoutFinalizeMissing");
  }
  let runErrored = false;
  let runPhase = "prepare";
  try {
    const prepareMethod = jvm.resolveVirtual(effectiveTestObject.__class, baseTestPrepareMethodId);
    if (typeof prepareMethod === "function") {
      yield* prepareMethod(effectiveTestObject);
    }
    runPhase = "runTest";
    const runTestMethod = jvm.resolveVirtual(effectiveTestObject.__class, baseTestRunTestMethodId);
    if (typeof runTestMethod === "function") {
      yield* runTestMethod(effectiveTestObject);
    }
  } catch (err) {
    runErrored = true;
    let errText = null;
    try {
      errText = yield* stringifyThrowable(err);
    } catch (_stringifyErr) {
      errText = String(err && err.message ? err.message : err);
    }
    if ((!errText || errText === "[object Object]") && err && typeof err === "object") {
      try {
        const errKeys = Object.keys(err);
        if (errKeys.length > 0) {
          errText = (errText || "[object Object]") + " keys=" + errKeys.slice(0, 8).join(",");
        }
      } catch (_keyErr) {
        // Best effort only.
      }
      if (err && err.__class) {
        errText = (errText || "[object Object]") + " class=" + String(err.__class);
      }
    }
    const errStack = err && err.stack ? String(err.stack) : "";
    if (global.console && typeof global.console.log === "function") {
      global.console.log("CN1SS:ERR:suite test=" + nativeTestName + " failed=" + errText + " phase=" + runPhase);
    }
    emitLambdaBridgeDiag("PARPAR:DIAG:FALLBACK:lambdaBridge:runError:phase=" + runPhase + ":error=" + errText);
    if (errStack) {
      emitLambdaBridgeDiag("PARPAR:DIAG:FALLBACK:lambdaBridge:runErrorStack=" + errStack.substring(0, 320));
      if (cn1ssLambdaRunErrorStackCount < 6 && global.console && typeof global.console.log === "function") {
        cn1ssLambdaRunErrorStackCount++;
        global.console.log("PARPAR:RUN_ERROR_STACK:test=" + nativeTestName + ":phase=" + runPhase + ":" + errStack);
      }
    }
    try {
      const failMethod = jvm.resolveVirtual(effectiveTestObject.__class, baseTestFailMethodId);
      if (typeof failMethod === "function") {
        yield* failMethod(effectiveTestObject, cn1ssToJavaString(errText));
      }
    } catch (_failErr) {
      // Best effort only.
    }
  }
  // Mirror Java's runNextTest lambda: after prepare()+runTest() (or catch),
  // delegate to awaitTestCompletion which polls isDone() and handles the
  // finalize+timeout logic. Skipping this step finalizes the test before
  // onShowCompleted→UITimer→emitCurrentFormScreenshot→done() can run, so no
  // screenshot is ever emitted for tests routed through this helper.
  try {
    const awaitMethod = jvm.resolveVirtual(callTarget.__class, cn1ssRunnerAwaitTestCompletionMethodId);
    if (typeof awaitMethod === "function") {
      const deadline = Date.now() + cn1ssTestTimeoutMs;
      return yield* awaitMethod(
        callTarget,
        effectiveIndex,
        effectiveTestObject,
        normalizedTestName,
        deadline
      );
    }
    emitLambdaBridgeDiag("PARPAR:DIAG:FALLBACK:lambdaBridge:awaitTestCompletionMissing=1");
  } catch (_awaitErr) {
    const awaitErrDetail = yield* stringifyThrowable(_awaitErr);
    emitLambdaBridgeDiag("PARPAR:DIAG:FALLBACK:lambdaBridge:awaitTestCompletionError=" + awaitErrDetail);
  }
  // Fallback to direct finalize if awaitTestCompletion isn't available.
  try {
    const finalizeMethod = jvm.resolveVirtual(callTarget.__class, cn1ssRunnerFinalizeTestMethodId);
    if (typeof finalizeMethod === "function") {
      return yield* finalizeMethod(
        callTarget,
        effectiveIndex,
        effectiveTestObject,
        normalizedTestName,
        0
      );
    }
    return yield* forceAdvanceCn1ssRunner(callTarget, effectiveIndex, "directFinalizeMissing");
  } catch (_finalizeAfterRunErr) {
    const finalizeErrDetail = yield* stringifyThrowable(_finalizeAfterRunErr);
    emitLambdaBridgeDiag("PARPAR:DIAG:FALLBACK:lambdaBridge:directFinalizeError=" + finalizeErrDetail);
    return yield* forceAdvanceCn1ssRunner(callTarget, effectiveIndex, "directFinalizeFailed");
  }
}

bindCiFallback("Cn1ssDeviceRunner.lambda1RunBridge", [
  cn1ssRunnerLambda1RunMethodId
], function*(__cn1ThisObject) {
  const runner = getCn1ssLambdaCaptureValue(__cn1ThisObject, 1);
  const testName = getCn1ssLambdaCaptureValue(__cn1ThisObject, 2);
  const index = getCn1ssLambdaCaptureValue(__cn1ThisObject, 3);
  const testObject = getCn1ssLambdaCaptureValue(__cn1ThisObject, 4);
  if (!runner || runner.__class !== cn1ssRunnerClassId) {
    emitLambdaBridgeDiag("PARPAR:DIAG:FALLBACK:lambda1RunBridge:missingDispatch=1");
    return null;
  }
  emitLambdaBridgeDiag(
    "PARPAR:DIAG:FALLBACK:lambda1RunBridge:dispatch:index=" + String(index == null ? "null" : (index | 0))
    + ":test=" + (testObject && testObject.__class ? testObject.__class : "null")
  );
  return yield* runCn1ssResolvedTest(runner, testObject, testName, index | 0);
});

bindCiFallback("Cn1ssDeviceRunner.lambda2RunBridge", [
  cn1ssRunnerLambda2RunMethodId
], function*(__cn1ThisObject) {
  const runner = getCn1ssLambdaCaptureValue(__cn1ThisObject, 1);
  const index = getCn1ssLambdaCaptureValue(__cn1ThisObject, 2);
  const testObject = getCn1ssLambdaCaptureValue(__cn1ThisObject, 3);
  const testName = getCn1ssLambdaCaptureValue(__cn1ThisObject, 4);
  const deadline = getCn1ssLambdaCaptureValue(__cn1ThisObject, 5);
  const awaitLambdaMethod = resolveCn1ssRunnerTranslatedMethod(
    [cn1ssRunnerAwaitLambda3MethodId],
    "Cn1ssDeviceRunner.lambda2RunBridge"
  );
  if (!runner || runner.__class !== cn1ssRunnerClassId || typeof awaitLambdaMethod !== "function") {
    emitLambdaBridgeDiag("PARPAR:DIAG:FALLBACK:lambda2RunBridge:missingDispatch=1");
    return null;
  }
  emitLambdaBridgeDiag(
    "PARPAR:DIAG:FALLBACK:lambda2RunBridge:dispatch:index=" + String(index == null ? "null" : (index | 0))
    + ":test=" + (testObject && testObject.__class ? testObject.__class : "null")
  );
  return yield* awaitLambdaMethod(runner, index | 0, testObject, testName, deadline);
});

function emitGuaranteedConsole(line) {
  if (global.console && typeof global.console.log === "function") {
    global.console.log(line);
  }
}

function* invokeCn1ssFinishSuite(runner, reason) {
  let finishErr = null;
  try {
    const finishSuiteMethod = jvm.resolveVirtual(runner.__class, cn1ssRunnerFinishSuiteMethodId);
    if (typeof finishSuiteMethod === "function") {
      emitGuaranteedConsole("CN1SS:INFO:lambda3RunBridge:finishSuiteInvoked reason=" + String(reason || "unknown"));
      return yield* finishSuiteMethod(runner);
    }
    emitGuaranteedConsole("CN1SS:ERR:lambda3RunBridge:finishSuiteMissing reason=" + String(reason || "unknown"));
  } catch (err) {
    finishErr = err;
    emitGuaranteedConsole("CN1SS:ERR:lambda3RunBridge:finishSuiteError reason=" + String(reason || "unknown")
      + " error=" + String(err && err.message ? err.message : err));
  }
  // Guaranteed terminator — even if finishSuite threw, ensure the harness can observe completion.
  emitGuaranteedConsole("CN1SS:INFO:swift_diag_status=unknown");
  emitGuaranteedConsole("CN1SS:SUITE:FINISHED");
  if (finishErr) {
    emitGuaranteedConsole("CN1SS:ERR:lambda3RunBridge:finishSuiteStack="
      + String(finishErr && finishErr.stack ? finishErr.stack : "none").substring(0, 512));
  }
  return null;
}

bindCiFallback("Cn1ssDeviceRunner.lambda3RunBridge", [
  cn1ssRunnerLambda3RunMethodId
], function*(__cn1ThisObject) {
  const runner = getCn1ssLambdaCaptureValue(__cn1ThisObject, 1);
  const testName = getCn1ssLambdaCaptureValue(__cn1ThisObject, 2);
  const index = getCn1ssLambdaCaptureValue(__cn1ThisObject, 3);
  if (!runner || runner.__class !== cn1ssRunnerClassId) {
    emitLambdaBridgeDiag("PARPAR:DIAG:FALLBACK:lambda3RunBridge:missingRunner=1");
    return null;
  }
  const nextIndex = ((index | 0) + 1) | 0;
  const nativeTestName = toCn1StringValue(testName);
  emitGuaranteedConsole("CN1SS:INFO:suite finished test=" + nativeTestName);
  emitLambdaBridgeDiag(
    "PARPAR:DIAG:FALLBACK:lambda3RunBridge:dispatch:index=" + String(index == null ? "null" : (index | 0))
    + ":nextIndex=" + String(nextIndex)
  );
  const totalTests = getCn1ssRunnerTestTotal();
  if (cn1ssSelectedTests && cn1ssSelectedTestMatched) {
    return yield* invokeCn1ssFinishSuite(runner, "filteredSuite");
  }
  if (totalTests > 0 && nextIndex >= totalTests) {
    return yield* invokeCn1ssFinishSuite(runner, "endOfSuite:nextIndex=" + nextIndex + ":total=" + totalTests);
  }
  try {
    const nextTestObject = resolveCn1ssIndexedTestObject(nextIndex);
    if (jvm.instanceOf(nextTestObject, "com_codenameone_examples_hellocodenameone_tests_BaseTest")) {
      return yield* runCn1ssResolvedTest(
        runner,
        nextTestObject,
        resolveCn1ssTestNameObject(nextTestObject, null),
        nextIndex
      );
    }
    const runNextTestMethod = jvm.resolveVirtual(runner.__class, cn1ssRunnerRunNextTestMethodId);
    if (typeof runNextTestMethod === "function") {
      return yield* runNextTestMethod(runner, nextIndex);
    }
    emitGuaranteedConsole("CN1SS:ERR:lambda3RunBridge:runNextTestMissing nextIndex=" + nextIndex);
  } catch (err) {
    emitGuaranteedConsole("CN1SS:ERR:lambda3RunBridge:runNextError nextIndex=" + nextIndex
      + " error=" + String(err && err.message ? err.message : err));
    if (err && err.stack) {
      emitGuaranteedConsole("CN1SS:ERR:lambda3RunBridge:runNextStack=" + String(err.stack).substring(0, 512));
    }
  }
  // If we got here, advancement failed — fall back to finishing the suite so the harness doesn't hang.
  return yield* invokeCn1ssFinishSuite(runner, "runNextFailed:nextIndex=" + nextIndex);
});

let cn1ssLambdaRunErrorStackCount = 0;
function emitLambdaBridgeDiag(line) {
  emitDiagLine(line);
}
function* forceAdvanceCn1ssRunner(callTarget, currentIndex, reason) {
  if (!callTarget || callTarget.__class !== cn1ssRunnerClassId) {
    return null;
  }
  const nextIndex = ((currentIndex | 0) + 1) | 0;
  callTarget.__cn1ForcedNextIndex = nextIndex;
  emitLambdaBridgeDiag(
    "PARPAR:DIAG:FALLBACK:lambdaBridge:forceAdvance:reason=" + String(reason || "unknown")
    + ":nextIndex=" + String(nextIndex)
  );
  const totalTests = getCn1ssRunnerTestTotal();
  if (totalTests > 0 && nextIndex >= totalTests) {
    return yield* invokeCn1ssFinishSuite(callTarget, "forceAdvance:endOfSuite:" + String(reason || "unknown"));
  }
  try {
    const runNextTestMethod = jvm.resolveVirtual(callTarget.__class, cn1ssRunnerRunNextTestMethodId);
    if (typeof runNextTestMethod === "function") {
      return yield* runNextTestMethod(callTarget, nextIndex);
    }
  } catch (advanceErr) {
    emitGuaranteedConsole(
      "CN1SS:ERR:lambdaBridge:forceAdvanceError reason=" + String(reason || "unknown")
      + " error=" + String(advanceErr && advanceErr.message ? advanceErr.message : advanceErr)
    );
  }
  return yield* invokeCn1ssFinishSuite(callTarget, "forceAdvance:runNextFailed:" + String(reason || "unknown"));
}

bindCiFallbackWithMethodId("Cn1ssDeviceRunner.lambdaRunNextTestBridge", cn1ssLambdaBridgeMethodIds, function*(invokedMethodId, __cn1ThisObject, arg1, arg2, arg3) {
  const signatureStringIntBaseTest = invokedMethodId
    && String(invokedMethodId).indexOf("_java_lang_String_int_com_codenameone_examples_hellocodenameone_tests_BaseTest") >= 0;
  const testName = arg1;
  const index = signatureStringIntBaseTest ? arg2 : arg3;
  const testObject = signatureStringIntBaseTest ? arg3 : arg2;
  const toSimpleClassName = function(classId) {
    const raw = String(classId || "");
    const pos = raw.lastIndexOf("_");
    return pos >= 0 ? raw.substring(pos + 1) : raw;
  };
  const toJavaString = function(value) {
    if (value && value.__class === "java_lang_String") {
      return value;
    }
    return jvm.createStringLiteral(String(value == null ? "" : value));
  };
  emitLambdaBridgeDiag(
    "PARPAR:DIAG:FALLBACK:lambdaBridge:receiver=" + (__cn1ThisObject && __cn1ThisObject.__class ? __cn1ThisObject.__class : "null")
    + ":arg1=" + (testName && testName.__class ? testName.__class : typeof testName)
    + ":arg2=" + (testObject && testObject.__class ? testObject.__class : typeof testObject)
    + ":arg3=" + (index == null ? "null" : String(index))
  );
  if (!__cn1ThisObject) {
    return null;
  }
  let extractedRunner = null;
  let capturedTestName = null;
  let capturedTestObject = null;
  let capturedIndex = null;
  const fieldKeys = __cn1ThisObject && typeof __cn1ThisObject === "object" ? Object.keys(__cn1ThisObject) : [];
  for (let i = 0; i < fieldKeys.length; i++) {
    const key = fieldKeys[i];
    const match = key.match(/Cn1ssDeviceRunner_lambda_\d+_arg_(\d+)$/);
    if (!match) {
      continue;
    }
    const ordinal = match[1];
    const value = __cn1ThisObject[key];
    if (ordinal === "1") {
      extractedRunner = value;
    } else if (ordinal === "2") {
      capturedTestName = value;
    } else if (ordinal === "3") {
      capturedTestObject = value;
    } else if (ordinal === "4") {
      capturedIndex = value;
    }
  }
  const runner = extractedRunner || (__cn1ThisObject.__class === cn1ssRunnerClassId ? __cn1ThisObject : null);
  let effectiveTestObject = testObject;
  if (!jvm.instanceOf(effectiveTestObject, "com_codenameone_examples_hellocodenameone_tests_BaseTest")) {
    if (jvm.instanceOf(capturedTestObject, "com_codenameone_examples_hellocodenameone_tests_BaseTest")) {
      effectiveTestObject = capturedTestObject;
    } else if (jvm.instanceOf(testName, "com_codenameone_examples_hellocodenameone_tests_BaseTest")) {
      effectiveTestObject = testName;
    }
  }
  if (!jvm.instanceOf(effectiveTestObject, "com_codenameone_examples_hellocodenameone_tests_BaseTest")) {
    const rawArgs = [arg1, arg2, arg3];
    for (let i = 0; i < rawArgs.length; i++) {
      if (jvm.instanceOf(rawArgs[i], "com_codenameone_examples_hellocodenameone_tests_BaseTest")) {
        effectiveTestObject = rawArgs[i];
        break;
      }
    }
  }
  let effectiveTestName = testName;
  if ((!effectiveTestName || effectiveTestName.__class !== "java_lang_String")) {
    if (capturedTestName && capturedTestName.__class === "java_lang_String") {
      effectiveTestName = capturedTestName;
    } else {
      const rawArgs = [arg1, arg2, arg3];
      for (let i = 0; i < rawArgs.length; i++) {
        const raw = rawArgs[i];
        if (raw && raw.__class === "java_lang_String") {
          effectiveTestName = raw;
          break;
        }
      }
    }
  }
  if (!effectiveTestName || effectiveTestName.__class !== "java_lang_String") {
    if (effectiveTestObject && effectiveTestObject.__class) {
      effectiveTestName = toJavaString(toSimpleClassName(effectiveTestObject.__class));
    } else {
      effectiveTestName = toJavaString("unknown");
    }
  }
  let effectiveIndex = index | 0;
  if (typeof index !== "number") {
    const rawArgs = [arg1, arg2, arg3];
    for (let i = 0; i < rawArgs.length; i++) {
      const raw = rawArgs[i];
      if (typeof raw === "number") {
        effectiveIndex = raw | 0;
        break;
      }
    }
    if (capturedIndex != null) {
      effectiveIndex = capturedIndex | 0;
    }
  } else if (capturedIndex != null && (capturedIndex | 0) !== effectiveIndex) {
    emitLambdaBridgeDiag(
      "PARPAR:DIAG:FALLBACK:lambdaBridge:indexFromArgsOverride:capture="
      + String(capturedIndex | 0) + ":effective=" + String(effectiveIndex)
    );
  }
  if (runner && typeof runner.__cn1ForcedNextIndex === "number" && (runner.__cn1ForcedNextIndex | 0) > effectiveIndex) {
    effectiveIndex = runner.__cn1ForcedNextIndex | 0;
    emitLambdaBridgeDiag(
      "PARPAR:DIAG:FALLBACK:lambdaBridge:indexForcedOverride="
      + String(effectiveIndex)
    );
  }
  const indexedTestObject = resolveCn1ssIndexedTestObject(effectiveIndex);
  if (jvm.instanceOf(indexedTestObject, "com_codenameone_examples_hellocodenameone_tests_BaseTest")) {
    if (!effectiveTestObject || !effectiveTestObject.__class || effectiveTestObject.__class !== indexedTestObject.__class) {
      emitLambdaBridgeDiag(
        "PARPAR:DIAG:FALLBACK:lambdaBridge:indexedOverride:index=" + String(effectiveIndex)
        + ":from=" + (effectiveTestObject && effectiveTestObject.__class ? effectiveTestObject.__class : "null")
        + ":to=" + indexedTestObject.__class
      );
    }
    effectiveTestObject = indexedTestObject;
  }
  emitLambdaBridgeDiag(
    "PARPAR:DIAG:FALLBACK:lambdaBridge:capturedRunner="
    + (runner && runner.__class ? runner.__class : "null")
    + ":capturedName=" + (effectiveTestName && effectiveTestName.__class ? effectiveTestName.__class : typeof effectiveTestName)
    + ":capturedTest=" + (effectiveTestObject && effectiveTestObject.__class ? effectiveTestObject.__class : typeof effectiveTestObject)
    + ":capturedIndex=" + String(effectiveIndex)
  );
  if (!runner || !runner.__class) {
    return null;
  }
  if (!jvm.instanceOf(effectiveTestObject, "com_codenameone_examples_hellocodenameone_tests_BaseTest")) {
    return null;
  }
  const isRunnerLambda = __cn1ThisObject && __cn1ThisObject.__class
    && String(__cn1ThisObject.__class).indexOf("com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_lambda_") === 0;
  const callTarget = isRunnerLambda ? runner : __cn1ThisObject;
  if (!callTarget || callTarget.__class !== cn1ssRunnerClassId) {
    return null;
  }
  if (callTarget.__cn1LambdaBridgeDispatching) {
    emitLambdaBridgeDiag("PARPAR:DIAG:FALLBACK:lambdaBridge:reentry-guard=hit:method=" + String(invokedMethodId || "unknown"));
    return null;
  }
  if (!invokedMethodId || cn1ssLambdaBridgeMethodIds.indexOf(invokedMethodId) >= 0) {
    return yield* runCn1ssResolvedTest(callTarget, effectiveTestObject, effectiveTestName, effectiveIndex);
  }
  const cn1ssLambdaBridgeOriginalRunnerMethod = resolveCn1ssLambdaBridgeOriginalRunnerMethod(invokedMethodId);
  if (typeof cn1ssLambdaBridgeOriginalRunnerMethod !== "function") {
    emitLambdaBridgeDiag(
      "PARPAR:DIAG:FALLBACK:lambdaBridge:originalRunnerMethod=missing:method="
      + String(invokedMethodId || "unknown")
    );
    return null;
  }
  callTarget.__cn1LambdaBridgeDispatching = true;
  try {
    return yield* cn1ssLambdaBridgeOriginalRunnerMethod(callTarget, effectiveTestName, effectiveTestObject, effectiveIndex);
  } finally {
    callTarget.__cn1LambdaBridgeDispatching = false;
  }
});

function toCn1StringValue(value) {
  if (value && value.__class === "java_lang_String") {
    try {
      return jvm.toNativeString(value);
    } catch (_err) {
      return "";
    }
  }
  if (value == null) {
    return "";
  }
  return String(value);
}

function byteArrayToBase64(value) {
  if (!value || typeof value.length !== "number") {
    return "";
  }
  const len = value.length | 0;
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i++) {
    let b = value[i] | 0;
    if (b < 0) {
      b += 256;
    }
    bytes[i] = b & 0xff;
  }
  let binary = "";
  const chunkSize = 0x8000;
  for (let i = 0; i < bytes.length; i += chunkSize) {
    const sub = bytes.subarray(i, i + chunkSize);
    binary += String.fromCharCode.apply(null, sub);
  }
  if (typeof global.btoa === "function") {
    return global.btoa(binary);
  }
  if (typeof Buffer !== "undefined") {
    return Buffer.from(bytes).toString("base64");
  }
  return "";
}

function normalizeCn1ssTestName(raw) {
  const value = raw && raw.length > 0 ? raw : "default";
  const normalized = String(value).replace(/[^A-Za-z0-9_.-]/g, "_");
  return normalized.length > 0 ? normalized : "default";
}

function readCiQueryParam(name) {
  const loc = global.location || (global.window && global.window.location) || null;
  const rawSearch = (loc && loc.search) ? String(loc.search) : String(global.__cn1LocationSearch || "");
  if (!rawSearch) {
    return "";
  }
  const search = rawSearch.charAt(0) === "?" ? rawSearch.substring(1) : rawSearch;
  if (!search) {
    return "";
  }
  const pairs = search.split("&");
  for (let i = 0; i < pairs.length; i++) {
    const entry = pairs[i];
    if (!entry) {
      continue;
    }
    const eq = entry.indexOf("=");
    const key = decodeURIComponent((eq >= 0 ? entry.substring(0, eq) : entry).replace(/\+/g, " "));
    if (key !== name) {
      continue;
    }
    return decodeURIComponent((eq >= 0 ? entry.substring(eq + 1) : "").replace(/\+/g, " "));
  }
  return "";
}

function toCn1ssFilterKey(raw) {
  return normalizeCn1ssTestName(raw).toLowerCase();
}

const cn1ssSelectedTests = (function() {
  const raw = readCiQueryParam("cn1ssTest");
  if (!raw) {
    return null;
  }
  const out = Object.create(null);
  const parts = String(raw).split(",");
  for (let i = 0; i < parts.length; i++) {
    const key = toCn1ssFilterKey(parts[i]);
    if (key && key !== "default") {
      out[key] = true;
    }
  }
  return Object.keys(out).length ? out : null;
})();

const cn1ssChunkIndexByStream = Object.create(null);
const cn1ssScreenshotEmitted = Object.create(null);
let cn1ssActiveTestName = "default";
let cn1ssActiveTestObject = null;
let cn1ssSelectedTestMatched = false;

function simpleCn1ssClassName(classId) {
  const raw = String(classId || "");
  const pos = raw.lastIndexOf("_");
  return pos >= 0 ? raw.substring(pos + 1) : raw;
}

function shouldRunCn1ssTest(nativeTestName, testObject) {
  if (!cn1ssSelectedTests) {
    return true;
  }
  const candidates = Object.create(null);
  candidates[toCn1ssFilterKey(nativeTestName)] = true;
  if (testObject && testObject.__class) {
    candidates[toCn1ssFilterKey(simpleCn1ssClassName(testObject.__class))] = true;
  }
  const keys = Object.keys(candidates);
  for (let i = 0; i < keys.length; i++) {
    if (cn1ssSelectedTests[keys[i]]) {
      return true;
    }
  }
  return false;
}

function resolveCn1ssTestName(raw) {
  const normalized = normalizeCn1ssTestName(raw);
  if (normalized !== "default") {
    return normalized;
  }
  const active = normalizeCn1ssTestName(cn1ssActiveTestName || "");
  return active !== "default" ? active : normalized;
}

function resolveBaseTestFromRunnable(runnable) {
  if (!runnable || typeof runnable !== "object") {
    return null;
  }
  if (typeof jvm.instanceOf === "function") {
    try {
      if (jvm.instanceOf(runnable, "com_codenameone_examples_hellocodenameone_tests_BaseTest")) {
        return runnable;
      }
    } catch (_err) {
      // Continue scanning captured fields.
    }
  }
  const keys = Object.keys(runnable);
  for (let i = 0; i < keys.length; i++) {
    const value = runnable[keys[i]];
    if (!value || typeof value !== "object") {
      continue;
    }
    if (typeof jvm.instanceOf === "function") {
      try {
        if (jvm.instanceOf(value, "com_codenameone_examples_hellocodenameone_tests_BaseTest")) {
          return value;
        }
      } catch (_err) {
        // Continue scanning.
      }
    }
  }
  return null;
}

function emitCn1ssChunks(base64, testName, channelName) {
  const channel = channelName ? String(channelName).toUpperCase() : "";
  const prefix = "CN1SS" + channel;
  const test = normalizeCn1ssTestName(testName);
  const streamKey = channel + "|" + test;
  let nextIndex = cn1ssChunkIndexByStream[streamKey] || 0;
  const chunkSize = 8000;
  for (let offset = 0; offset < base64.length; offset += chunkSize) {
    const payload = base64.substring(offset, offset + chunkSize);
    const index = String(nextIndex++).padStart(6, "0");
    emitDiagLine(prefix + ":" + test + ":" + index + ":" + payload);
  }
  cn1ssChunkIndexByStream[streamKey] = nextIndex;
  if (base64.length === 0) {
    const index = String(nextIndex).padStart(6, "0");
    emitDiagLine(prefix + ":" + test + ":" + index + ":");
    cn1ssChunkIndexByStream[streamKey] = nextIndex + 1;
  }
  // Emit END marker matching the Java emitChannel convention so the
  // downstream cn1ss_list_tests / cn1ss_decode helpers can detect the stream.
  emitDiagLine(prefix + ":END:" + test);
}

const cn1ssEmitCurrentFormScreenshotMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunnerHelper_emitCurrentFormScreenshot_java_lang_String_java_lang_Runnable";
const cn1ssHelperClassName = "com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunnerHelper";
let cn1ssEmitCurrentFormScreenshotInvokeDepth = 0;

function isFallbackFunctionForSymbol(fn, symbol) {
  return !!(fn && fn.__cn1CiFallbackSymbol === symbol);
}

function resolveTranslatedMethodCandidate(methodIds, ownerClassName, fallbackSymbol) {
  const translatedMethods = jvm && jvm.translatedMethods ? jvm.translatedMethods : null;
  if (translatedMethods) {
    for (let i = 0; i < methodIds.length; i++) {
      const methodId = methodIds[i];
      const candidate = translatedMethods[methodId];
      if (typeof candidate === "function" && !isFallbackFunctionForSymbol(candidate, fallbackSymbol)) {
        return { fn: candidate, source: "translated:" + methodId };
      }
    }
  }
  for (let i = 0; i < methodIds.length; i++) {
    const methodId = methodIds[i];
    const candidate = global[methodId];
    if (typeof candidate === "function" && !isFallbackFunctionForSymbol(candidate, fallbackSymbol)) {
      return { fn: candidate, source: "global:" + methodId };
    }
  }
  const ownerClass = jvm && jvm.classes ? jvm.classes[ownerClassName] : null;
  const methods = ownerClass && ownerClass.methods ? ownerClass.methods : null;
  if (methods) {
    for (let i = 0; i < methodIds.length; i++) {
      const methodId = methodIds[i];
      const candidate = methods[methodId];
      if (typeof candidate === "function" && !isFallbackFunctionForSymbol(candidate, fallbackSymbol)) {
        return { fn: candidate, source: "class:" + ownerClassName + ":" + methodId };
      }
    }
  }
  return null;
}

function isInstanceAssignableTo(value, targetClassName) {
  if (!value || !value.__class || !targetClassName) {
    return false;
  }
  const classDef = value.__classDef || (jvm && jvm.classes ? jvm.classes[value.__class] : null);
  const assignableTo = classDef && classDef.assignableTo ? classDef.assignableTo : null;
  return !!(assignableTo && assignableTo[targetClassName]);
}

function resolveDisplaySingleton() {
  const displayClass = jvm && jvm.classes ? jvm.classes["com_codename1_ui_Display"] : null;
  const staticFields = displayClass && displayClass.staticFields ? displayClass.staticFields : null;
  if (!staticFields) {
    return null;
  }
  const instance = staticFields["INSTANCE"];
  return instance && instance.__class ? instance : null;
}

function resolveDisplayImplementationObject() {
  const displayClass = jvm && jvm.classes ? jvm.classes["com_codename1_ui_Display"] : null;
  const staticFields = displayClass && displayClass.staticFields ? displayClass.staticFields : null;
  if (!staticFields) {
    return null;
  }
  const exact = staticFields["impl"];
  if (exact && exact.__class) {
    return exact;
  }
  const keys = Object.keys(staticFields);
  for (let i = 0; i < keys.length; i++) {
    const value = staticFields[keys[i]];
    if (!value || !value.__class) {
      continue;
    }
    if (isInstanceAssignableTo(value, "com_codename1_impl_CodenameOneImplementation")) {
      return value;
    }
  }
  return null;
}

function* invokeFirstResolvableInstanceMethod(receiver, methodIds) {
  if (!receiver || !receiver.__class || !methodIds || !methodIds.length) {
    return null;
  }
  for (let i = 0; i < methodIds.length; i++) {
    const methodId = methodIds[i];
    try {
      const method = jvm.resolveVirtual(receiver.__class, methodId);
      if (typeof method === "function") {
        yield* method(receiver);
        return methodId;
      }
    } catch (_err) {
      // Best-effort compatibility shim. Try the next translated name.
    }
  }
  return null;
}

function* forceDisplayPresentationForScreenshot(reason) {
  const display = resolveDisplaySingleton();
  const impl = resolveDisplayImplementationObject();
  const invoked = [];
  if (display && display.__class) {
    const displayMethodId = yield* invokeFirstResolvableInstanceMethod(display, [
      "cn1_com_codename1_ui_Display_flushEdt",
      "cn1_com_codename1_ui_Display_flushEdt__"
    ]);
    if (displayMethodId) {
      invoked.push("display:" + displayMethodId);
    }
  }
  if (impl && impl.__class) {
    const paintMethodId = yield* invokeFirstResolvableInstanceMethod(impl, [
      "cn1_com_codename1_impl_CodenameOneImplementation_paintDirty",
      "cn1_com_codename1_impl_CodenameOneImplementation_paintDirty__",
      "cn1_com_codename1_impl_html5_HTML5Implementation_paintDirty",
      "cn1_com_codename1_impl_html5_HTML5Implementation_paintDirty__"
    ]);
    if (paintMethodId) {
      invoked.push("impl:" + paintMethodId);
    }
    const flushMethodId = yield* invokeFirstResolvableInstanceMethod(impl, [
      "cn1_com_codename1_impl_html5_HTML5Implementation_flushGraphics",
      "cn1_com_codename1_impl_html5_HTML5Implementation_flushGraphics__",
      "cn1_com_codename1_impl_CodenameOneImplementation_flushGraphics",
      "cn1_com_codename1_impl_CodenameOneImplementation_flushGraphics__"
    ]);
    if (flushMethodId) {
      invoked.push("impl:" + flushMethodId);
    }
  }
  emitDiagLine(
    "PARPAR:DIAG:FALLBACK:forceDisplayPresentation:reason=" + String(reason || "unknown")
    + ":display=" + (display && display.__class ? display.__class : "null")
    + ":impl=" + (impl && impl.__class ? impl.__class : "null")
    + ":invoked=" + (invoked.length ? invoked.join(",") : "none")
  );
  return invoked.length > 0;
}

bindCiFallback("Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshotDom", [
  cn1ssEmitCurrentFormScreenshotMethodId,
  cn1ssEmitCurrentFormScreenshotMethodId + "__impl"
], function*(testName, completion) {
  const fallbackSymbol = "Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshotDom";
  const test = toCn1StringValue(testName);
  const normalizedTest = resolveCn1ssTestName(test);
  let shouldUseDomFallback = true;
  const originalResolved = resolveTranslatedMethodCandidate([
    // Prefer translated __impl first. The non-impl wrapper may dispatch via
    // rebound globals and recurse into this fallback.
    cn1ssEmitCurrentFormScreenshotMethodId + "__impl",
    cn1ssEmitCurrentFormScreenshotMethodId
  ], cn1ssHelperClassName, fallbackSymbol);
  // In worker mode the translated screenshot path eventually calls
  // BlobUtil.canvasToBlob() which uses HTMLCanvasElement.toBlob(callback).
  // That callback is a Java object and cannot be invoked from the host
  // thread, so the worker hangs forever in a wait-loop.  Always use the
  // DOM-based capture via host bridge calls instead – this avoids async
  // callbacks entirely and works reliably across the worker boundary.
  if (originalResolved && typeof originalResolved.fn === "function") {
    emitDiagLine("PARPAR:DIAG:FALLBACK:cn1ssEmitCurrentFormScreenshotDom:skipTranslated=canvasToBlob_hang");
  } else {
    emitDiagLine("PARPAR:DIAG:FALLBACK:cn1ssEmitCurrentFormScreenshotDom:originalMissing=1");
  }
  const canvas = global.document && typeof global.document.querySelector === "function"
    ? global.document.querySelector("canvas")
    : null;
  if (cn1ssScreenshotEmitted[normalizedTest]) {
    emitDiagLine("PARPAR:DIAG:FALLBACK:cn1ssEmitCurrentFormScreenshotDom:skipDuplicate=" + normalizedTest);
  } else if (shouldUseDomFallback && canvas && typeof canvas.toDataURL === "function") {
    yield* forceDisplayPresentationForScreenshot("domCanvas:" + normalizedTest);
    cn1ssScreenshotEmitted[normalizedTest] = true;
    const dataUrl = String(canvas.toDataURL("image/png") || "");
    const comma = dataUrl.indexOf(",");
    const base64 = comma >= 0 ? dataUrl.substring(comma + 1) : "";
    emitCn1ssChunks(base64, normalizedTest, "");
  } else if (shouldUseDomFallback) {
    let capturedDataUrl = "";
    if (jvm && typeof jvm.invokeHostNative === "function") {
      try {
        yield* forceDisplayPresentationForScreenshot("hostCanvas:" + normalizedTest);
        yield jvm.invokeHostNative("__cn1_wait_for_ui_settle__", [{
          reason: "screenshot:" + normalizedTest,
          maxFrames: 48,
          stableFrames: 3,
          quietFrames: 3
        }]);
        const hostResult = yield jvm.invokeHostNative("__cn1_capture_canvas_png__", []);
        capturedDataUrl = hostResult == null ? "" : String(hostResult);
      } catch (_hostCaptureErr) {
        capturedDataUrl = "";
      }
    }
    if (capturedDataUrl && capturedDataUrl.indexOf("data:image/") === 0) {
      cn1ssScreenshotEmitted[normalizedTest] = true;
      const comma = capturedDataUrl.indexOf(",");
      const base64 = comma >= 0 ? capturedDataUrl.substring(comma + 1) : "";
      emitDiagLine("PARPAR:DIAG:FALLBACK:cn1ssEmitCurrentFormScreenshotDom:hostCanvas=1:test=" + normalizedTest);
      emitCn1ssChunks(base64, normalizedTest, "");
    } else {
      emitDiagLine("PARPAR:DIAG:FALLBACK:cn1ssEmitCurrentFormScreenshotDom:noCanvas=1:test=" + normalizedTest);
    }
  }
  let completionRunnableRan = false;
  if (completion && completion.__class) {
    try {
      const runMethod = jvm.resolveVirtual(completion.__class, "cn1_java_lang_Runnable_run");
      yield* runMethod(completion);
      completionRunnableRan = true;
    } catch (err) {
      emitDiagLine("PARPAR:DIAG:FALLBACK:cn1ssEmitCurrentFormScreenshotDom:completionRunErr=" + String(err && err.message ? err.message : err));
    }
  }
  const baseTest = resolveBaseTestFromRunnable(completion);
  const effectiveBaseTest = (baseTest && baseTest.__class)
    ? baseTest
    : (cn1ssActiveTestObject && cn1ssActiveTestObject.__class ? cn1ssActiveTestObject : null);
  if (effectiveBaseTest && effectiveBaseTest.__class) {
    try {
      const isDoneMethod = jvm.resolveVirtual(effectiveBaseTest.__class, "cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_isDone_R_boolean");
      const alreadyDone = ((yield* isDoneMethod(effectiveBaseTest)) | 0) !== 0;
      if (!alreadyDone) {
        const doneMethod = jvm.resolveVirtual(effectiveBaseTest.__class, baseTestDoneMethodId);
        yield* doneMethod(effectiveBaseTest);
        emitDiagLine(
          "PARPAR:DIAG:FALLBACK:cn1ssEmitCurrentFormScreenshotDom:forcedDone=1:completionRun="
          + (completionRunnableRan ? "1" : "0")
          + ":source=" + (baseTest ? "completion" : "activeTest")
        );
      }
    } catch (err) {
      emitDiagLine("PARPAR:DIAG:FALLBACK:cn1ssEmitCurrentFormScreenshotDom:forcedDoneErr=" + String(err && err.message ? err.message : err));
    }
  }
  return null;
});

bindCiFallback("Cn1ssDeviceRunnerHelper.emitChannelFastJs", [
  cn1ssEmitChannelMethodId,
  cn1ssEmitChannelMethodId + "__impl"
], function*(payloadBytes, testName, channelName) {
  const test = resolveCn1ssTestName(toCn1StringValue(testName));
  const channel = toCn1StringValue(channelName);
  // For the primary screenshot channel (empty channel name), the Java-side
  // Display.screenshot() in the worker reads from OffscreenCanvas which
  // may not reflect the main-thread visible canvas.  Replace the payload
  // with a main-thread canvas capture via the host bridge when available.
  if (!channel && jvm && typeof jvm.invokeHostNative === "function" && !cn1ssScreenshotEmitted[test]) {
    try {
      yield* forceDisplayPresentationForScreenshot("emitChannel:" + test);
      yield jvm.invokeHostNative("__cn1_wait_for_ui_settle__", [{
        reason: "screenshot:" + test,
        maxFrames: 48,
        stableFrames: 3,
        quietFrames: 3
      }]);
      const hostResult = yield jvm.invokeHostNative("__cn1_capture_canvas_png__", []);
      const capturedDataUrl = hostResult == null ? "" : String(hostResult);
      if (capturedDataUrl && capturedDataUrl.indexOf("data:image/") === 0) {
        cn1ssScreenshotEmitted[test] = true;
        const comma = capturedDataUrl.indexOf(",");
        const hostBase64 = comma >= 0 ? capturedDataUrl.substring(comma + 1) : "";
        emitDiagLine("PARPAR:DIAG:FALLBACK:emitChannelFastJs:hostCapture=1:test=" + test + ":len=" + hostBase64.length);
        emitCn1ssChunks(hostBase64, test, channel);
        return null;
      }
    } catch (_hostErr) {
      emitDiagLine("PARPAR:DIAG:FALLBACK:emitChannelFastJs:hostCaptureErr=" + String(_hostErr && _hostErr.message ? _hostErr.message : _hostErr));
    }
  }
  const base64 = byteArrayToBase64(payloadBytes);
  emitCn1ssChunks(base64, test, channel);
  return null;
});

bindCiFallback("Cn1ssDeviceRunnerHelper.completeNullRunnableGuard", [
  cn1ssCompleteMethodId,
  cn1ssCompleteMethodId + "__impl"
], function*(completion) {
  if (!completion || !completion.__class) {
    emitDiagLine("PARPAR:DIAG:FALLBACK:cn1ssComplete:nullOrClasslessRunnable=1");
    return null;
  }
  const runMethod = jvm.resolveVirtual(completion.__class, "cn1_java_lang_Runnable_run");
  return yield* runMethod(completion);
});

bindCiFallback("BaseTest.registerReadyCallbackImmediate", [
  baseTestRegisterReadyCallbackMethodId,
  baseTestRegisterReadyCallbackMethodId + "__impl"
], function*(_baseTest, _form, callback) {
  const activeTest = normalizeCn1ssTestName(cn1ssActiveTestName || "default");
  let settleChanged = "na";
  let delayMillis = 1500;
  if (activeTest === "DrawImage" || activeTest === "graphics-draw-image-rect") {
    delayMillis = 4000;
  }
  if (jvm && typeof jvm.invokeHostNative === "function") {
    try {
      yield jvm.invokeHostNative("__cn1_delay__", [{ millis: delayMillis }]);
      const settleResult = yield jvm.invokeHostNative("__cn1_wait_for_ui_settle__", [{
        reason: "ready:" + activeTest,
        maxFrames: delayMillis > 1500 ? 120 : 48,
        stableFrames: delayMillis > 1500 ? 6 : 3,
        quietFrames: delayMillis > 1500 ? 6 : 3
      }]);
      if (settleResult && settleResult.changedFromPrevious != null) {
        settleChanged = String((settleResult.changedFromPrevious | 0) !== 0 ? 1 : 0);
      }
    } catch (_settleErr) {
      settleChanged = "err";
    }
  }
  emitDiagLine("PARPAR:DIAG:FALLBACK:baseTestRegisterReady:afterUiSettle=1:test=" + activeTest + ":delayMs=" + delayMillis + ":changed=" + settleChanged);
  if (!callback || !callback.__class) {
    return null;
  }
  const runMethod = jvm.resolveVirtual(callback.__class, "cn1_java_lang_Runnable_run");
  return yield* runMethod(callback);
});

const baseTestOnShowLambdaMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_1_lambda_onShowCompleted_0_java_lang_String";
const baseTestOnShowLambdaCarrierClass = "com_codenameone_examples_hellocodenameone_tests_BaseTest_1_lambda_0";
function installBaseTestOnShowLambdaShim() {
  if (!(jvm && typeof jvm.addVirtualMethod === "function" && jvm.classes && jvm.classes[baseTestOnShowLambdaCarrierClass])) {
    return false;
  }
  const carrierMethods = jvm.classes[baseTestOnShowLambdaCarrierClass].methods || {};
  if (typeof carrierMethods[baseTestOnShowLambdaMethodId] === "function") {
    return true;
  }
  jvm.addVirtualMethod(baseTestOnShowLambdaCarrierClass, baseTestOnShowLambdaMethodId, function*(__cn1ThisObject, onShowMessage) {
    const target = __cn1ThisObject
      ? (__cn1ThisObject["cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_1_lambda_0_arg_1"] || __cn1ThisObject)
      : null;
    if (!target || !target.__class) {
      return null;
    }
    const classDef = target.__classDef || (jvm.classes ? jvm.classes[target.__class] : null);
    if (!classDef) {
      emitDiagLine("PARPAR:DIAG:FALLBACK:baseTestOnShowLambda:noClassDef=1:class=" + String(target.__class || "null"));
      return null;
    }
    let method = (classDef && classDef.methods) ? classDef.methods[baseTestOnShowLambdaMethodId] : null;
    if (!method) {
      method = jvm.resolveVirtual(target.__class, baseTestOnShowLambdaMethodId);
    }
    return yield* method(target, onShowMessage);
  });
  emitDiagLine("PARPAR:DIAG:INIT:shim=baseTestOnShowLambdaDispatch");
  return true;
}
if (!installBaseTestOnShowLambdaShim() && typeof setTimeout === "function") {
  setTimeout(function() {
    if (installBaseTestOnShowLambdaShim()) {
      emitDiagLine("PARPAR:DIAG:INIT:shim=baseTestOnShowLambdaDispatch:deferred=1");
    }
  }, 0);
}

// ---------------------------------------------------------------------------
// Shim: CodenameOneImplementation.initImpl – guard against getClass()/getName()
// failures on the Runnable argument passed to Display.init().
//
// In the ParparVM JS translation, Object.getClass() may return null or
// Class.getName() may return a name with underscores instead of dots.
// The base initImpl calls m.getClass().getName() and then
// String.substring(0, String.lastIndexOf('.')) which can throw a TypeError
// (null receiver) or StringIndexOutOfBoundsException (-1 index).
//
// This shim wraps the original initImpl; if it fails it falls back to calling
// init(m) directly and setting the packageName field from the class name of the
// bootstrap object.
// ---------------------------------------------------------------------------
const initImplMethodId = "cn1_com_codename1_impl_CodenameOneImplementation_initImpl_java_lang_Object";
const initImplOriginal = (function() {
  if (!jvm || !jvm.classes) {
    return null;
  }
  const cls = jvm.classes["com_codename1_impl_CodenameOneImplementation"];
  if (cls && cls.methods && typeof cls.methods[initImplMethodId] === "function") {
    return cls.methods[initImplMethodId];
  }
  return typeof global[initImplMethodId] === "function" ? global[initImplMethodId] :
         typeof global[initImplMethodId + "__impl"] === "function" ? global[initImplMethodId + "__impl"] : null;
})();

bindCiFallback("CodenameOneImplementation.initImplSafe", [
  initImplMethodId,
  initImplMethodId + "__impl"
], function*(__cn1ThisObject, m) {
  if (typeof initImplOriginal === "function") {
    try {
      return yield* initImplOriginal(__cn1ThisObject, m);
    } catch (err) {
      const message = String(err && err.message ? err.message : err || "");
      if (message.indexOf("__classDef") >= 0 || message.indexOf("lastIndexOf") >= 0 || message.indexOf("substring") >= 0) {
        emitCiFallbackMarker("CodenameOneImplementation.initImplSafe.recover", "HIT");
        // The original initImpl calls init(m) first, then m.getClass().getName().
        // If we land here, init(m) already succeeded – only the getClass/getName
        // chain failed.  Do NOT call init(m) again; just set the missing fields.
        const className = (m && m.__class) ? String(m.__class).replace(/_/g, ".") : "com.codename1.impl.html5";
        const dotIndex = className.lastIndexOf(".");
        const pkg = dotIndex >= 0 ? className.substring(0, dotIndex) : className;
        __cn1ThisObject["cn1_com_codename1_impl_CodenameOneImplementation_packageName"] = jvm.createStringLiteral(pkg);
        __cn1ThisObject["cn1_com_codename1_impl_CodenameOneImplementation_initiailized"] = 1;
        return null;
      }
      throw err;
    }
  }
  // No original method found – perform safe init inline
  const initMethodId2 = "cn1_com_codename1_impl_CodenameOneImplementation_init_java_lang_Object";
  try {
    const initMethod2 = jvm.resolveVirtual(__cn1ThisObject.__class, initMethodId2);
    if (typeof initMethod2 === "function") {
      yield* initMethod2(__cn1ThisObject, m);
    }
  } catch (_ignore) {
    // Best effort – init may already have been called
  }
  const className2 = (m && m.__class) ? String(m.__class).replace(/_/g, ".") : "com.codename1.impl.html5";
  const dotIndex2 = className2.lastIndexOf(".");
  const pkg2 = dotIndex2 >= 0 ? className2.substring(0, dotIndex2) : className2;
  __cn1ThisObject["cn1_com_codename1_impl_CodenameOneImplementation_packageName"] = jvm.createStringLiteral(pkg2);
  __cn1ThisObject["cn1_com_codename1_impl_CodenameOneImplementation_initiailized"] = 1;
  return null;
});

bindCiFallback("BrowserComponent.access102InternalAssignFix", [
  "cn1_com_codename1_ui_BrowserComponent_access_102_com_codename1_ui_BrowserComponent_com_codename1_ui_PeerComponent_R_com_codename1_ui_PeerComponent"
], function*(browserComponent, peerComponent) {
  if (browserComponent) {
    browserComponent["cn1_com_codename1_ui_BrowserComponent_internal"] = peerComponent;
  }
  return peerComponent;
});
