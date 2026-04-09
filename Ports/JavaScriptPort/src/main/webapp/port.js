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
  bindNative(names, function*() {
    emitCiFallbackMarker(symbol, "HIT");
    return yield* fn.apply(this, arguments);
  });
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

function emitDiagLine(line) {
  if (global.console && typeof global.console.log === "function") {
    global.console.log(line);
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
  const wrapper = jvm.wrapJsObject(global.window || global.self || global, "com_codename1_html5_js_browser_Window");
  jvm.enhanceJsWrapper(wrapper, "com_codename1_impl_html5_JSOImplementations_WindowExt");
  return wrapper;
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

bindNative([
  "cn1_com_codename1_impl_html5_HTML5Implementation_createCNOutboxEvent_java_lang_String_int_R_com_codename1_html5_js_dom_Event",
  "cn1_com_codename1_impl_html5_HTML5Implementation_createCNOutboxEvent___java_lang_String_int_R_com_codename1_html5_js_dom_Event"
], function*(message, code) {
  const win = global.window || global.self || global;
  const detail = message == null ? null : jvm.toNativeString(message);
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
  if (!nativeCanvas || typeof nativeCanvas.toDataURL !== "function") {
    emitDiagLine("PARPAR:DIAG:FALLBACK:blobUtilCanvasToBlob:missingCanvas=1");
    return null;
  }
  const mime = mimeType && mimeType.__class === "java_lang_String"
    ? jvm.toNativeString(mimeType)
    : (typeof mimeType === "string" ? mimeType : "image/png");
  const q = typeof quality === "number" ? quality : 0.92;
  let dataUrl = "";
  try {
    dataUrl = nativeCanvas.toDataURL(mime || "image/png", q);
  } catch (_err) {
    dataUrl = nativeCanvas.toDataURL("image/png");
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

bindCiFallback("Font.createTrueTypeFont", [
  "cn1_com_codename1_ui_Font_createTrueTypeFont_java_lang_String_java_lang_String_R_com_codename1_ui_Font"
], function*() {
  const getDefaultFont = global.cn1_com_codename1_ui_Font_getDefaultFont_R_com_codename1_ui_Font__impl
    || global.cn1_com_codename1_ui_Font_getDefaultFont_R_com_codename1_ui_Font;
  if (typeof getDefaultFont === "function") {
    return yield* getDefaultFont();
  }
  if (jvm.classes && jvm.classes["com_codename1_ui_Font"] && jvm.classes["com_codename1_ui_Font"].staticFields) {
    return jvm.classes["com_codename1_ui_Font"].staticFields["defaultFont"] || null;
  }
  return null;
});

const nativeFontGetCssMethodId = "cn1_com_codename1_impl_html5_HTML5Implementation_NativeFont_getCSS_R_java_lang_String";
const nativeFontCharWidthMethodId = "cn1_com_codename1_impl_html5_HTML5Implementation_NativeFont_charWidth_char_R_int";
const nativeFontGetCssOriginal = (jvm.classes
  && jvm.classes["com_codename1_impl_html5_HTML5Implementation_NativeFont"]
  && jvm.classes["com_codename1_impl_html5_HTML5Implementation_NativeFont"].methods)
  ? jvm.classes["com_codename1_impl_html5_HTML5Implementation_NativeFont"].methods[nativeFontGetCssMethodId]
  : null;
const nativeFontCharWidthOriginal = (jvm.classes
  && jvm.classes["com_codename1_impl_html5_HTML5Implementation_NativeFont"]
  && jvm.classes["com_codename1_impl_html5_HTML5Implementation_NativeFont"].methods)
  ? jvm.classes["com_codename1_impl_html5_HTML5Implementation_NativeFont"].methods[nativeFontCharWidthMethodId]
  : null;

bindCiFallback("NativeFont.getCSSNullSafe", [
  nativeFontGetCssMethodId
], function*(__cn1ThisObject) {
  if (typeof nativeFontGetCssOriginal !== "function") {
    return jvm.createStringLiteral("16px sans-serif");
  }
  try {
    return yield* nativeFontGetCssOriginal(__cn1ThisObject);
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
  if (typeof nativeFontCharWidthOriginal !== "function") {
    return 8;
  }
  try {
    return yield* nativeFontCharWidthOriginal(__cn1ThisObject, chr);
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
  if (typeof hashMapComputeHashCodeOriginal === "function") {
    return yield* hashMapComputeHashCodeOriginal(key);
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
const styleSetPaddingUnitOriginal = (jvm.classes && jvm.classes["com_codename1_ui_plaf_Style"] && jvm.classes["com_codename1_ui_plaf_Style"].methods)
  ? jvm.classes["com_codename1_ui_plaf_Style"].methods[styleSetPaddingUnitMethodId]
  : null;
const styleSetMarginUnitOriginal = (jvm.classes && jvm.classes["com_codename1_ui_plaf_Style"] && jvm.classes["com_codename1_ui_plaf_Style"].methods)
  ? jvm.classes["com_codename1_ui_plaf_Style"].methods[styleSetMarginUnitMethodId]
  : null;
const styleConvertUnitOriginal = (jvm.classes && jvm.classes["com_codename1_ui_plaf_Style"] && jvm.classes["com_codename1_ui_plaf_Style"].methods)
  ? jvm.classes["com_codename1_ui_plaf_Style"].methods[styleConvertUnitMethodId]
  : null;

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
  if (typeof styleSetPaddingUnitOriginal !== "function") {
    return null;
  }
  return yield* styleSetPaddingUnitOriginal(__cn1ThisObject, ensureJavaByteArray4(arr));
});

bindCiFallback("Style.setMarginUnitArrayCoerce", [
  styleSetMarginUnitMethodId
], function*(__cn1ThisObject, arr) {
  if (typeof styleSetMarginUnitOriginal !== "function") {
    return null;
  }
  return yield* styleSetMarginUnitOriginal(__cn1ThisObject, ensureJavaByteArray4(arr));
});

bindCiFallback("Style.convertUnitArrayCoerce", [
  styleConvertUnitMethodId
], function*(__cn1ThisObject, arr, value, side) {
  if (typeof styleConvertUnitOriginal !== "function") {
    return 0;
  }
  return yield* styleConvertUnitOriginal(__cn1ThisObject, ensureJavaByteArray4(arr), value, side);
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
const formInitLafOriginalMethod = (function() {
  if (!jvm || !jvm.classes || !jvm.classes["com_codename1_ui_Form"] || !jvm.classes["com_codename1_ui_Form"].methods) {
    return null;
  }
  const candidate = jvm.classes["com_codename1_ui_Form"].methods[formInitLafMethodId];
  return typeof candidate === "function" ? candidate : null;
})();
let formInitLafDiagCount = 0;
function emitFormInitLafDiag(line) {
  if (formInitLafDiagCount >= 80) {
    return;
  }
  formInitLafDiagCount++;
  emitDiagLine(line);
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

bindCiFallback("Form.initLafNullUiManagerBridge", [
  formInitLafMethodId
], function*(__cn1ThisObject, uiManager) {
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

const formCtorLayoutMethodId = "cn1_com_codename1_ui_Form___INIT___com_codename1_ui_layouts_Layout";
const formCtorTitleLayoutMethodId = "cn1_com_codename1_ui_Form___INIT___java_lang_String_com_codename1_ui_layouts_Layout";
const formCtorLayoutOriginal = typeof global[formCtorLayoutMethodId] === "function" ? global[formCtorLayoutMethodId] : null;
const formCtorTitleLayoutOriginal = typeof global[formCtorTitleLayoutMethodId] === "function" ? global[formCtorTitleLayoutMethodId] : null;
const formDefaultCtorMethodId = "cn1_com_codename1_ui_Form___INIT__";
const formSetTitleMethodId = "cn1_com_codename1_ui_Form_setTitle_java_lang_String";
const containerSetLayoutMethodId = "cn1_com_codename1_ui_Container_setLayout_com_codename1_ui_layouts_Layout";

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

const cn1ssCompleteMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunnerHelper_complete_java_lang_Runnable";
const cn1ssEmitChannelMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunnerHelper_emitChannel_byte_1ARRAY_java_lang_String_java_lang_String";
const baseTestRegisterReadyCallbackMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_registerReadyCallback_com_codename1_ui_Form_java_lang_Runnable";
const cn1ssRunnerClassId = "com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner";
const cn1ssRunnerListGetMethodId = "cn1_java_util_List_get_int_R_java_lang_Object";
function collectCn1ssRunnerLambdaMethodIds() {
  const ids = [];
  if (!jvm || !jvm.classes || !jvm.classes[cn1ssRunnerClassId] || !jvm.classes[cn1ssRunnerClassId].methods) {
    return ids;
  }
  const methods = jvm.classes[cn1ssRunnerClassId].methods;
  const prefix = "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_lambda_runNextTest_";
  for (const methodId in methods) {
    if (methodId.indexOf(prefix) === 0 && methodId.endsWith("_java_lang_String_com_codenameone_examples_hellocodenameone_tests_BaseTest_int")) {
      ids.push(methodId);
    }
  }
  ids.sort();
  return ids;
}
const cn1ssLambdaBridgeMethodIds = (function() {
  const collected = collectCn1ssRunnerLambdaMethodIds();
  if (collected.length > 0) {
    return collected;
  }
  return ["cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunner_lambda_runNextTest_2_java_lang_String_com_codenameone_examples_hellocodenameone_tests_BaseTest_int"];
})();
const cn1ssLambdaBridgeOriginalRunnerMethod = (function() {
  if (!jvm || !jvm.classes || !jvm.classes[cn1ssRunnerClassId] || !jvm.classes[cn1ssRunnerClassId].methods) {
    return null;
  }
  const methods = jvm.classes[cn1ssRunnerClassId].methods;
  for (let i = 0; i < cn1ssLambdaBridgeMethodIds.length; i++) {
    const candidate = methods[cn1ssLambdaBridgeMethodIds[i]];
    if (typeof candidate === "function") {
      return candidate;
    }
  }
  return null;
})();
let cn1ssLambdaBridgeDiagCount = 0;
function emitLambdaBridgeDiag(line) {
  if (cn1ssLambdaBridgeDiagCount >= 60) {
    return;
  }
  cn1ssLambdaBridgeDiagCount++;
  emitDiagLine(line);
}

bindCiFallback("Cn1ssDeviceRunner.lambdaRunNextTestBridge", cn1ssLambdaBridgeMethodIds, function*(__cn1ThisObject, testName, testObject, index) {
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
  let effectiveTestObject = capturedTestObject != null ? capturedTestObject : testObject;
  if (!jvm.instanceOf(effectiveTestObject, "com_codenameone_examples_hellocodenameone_tests_BaseTest")) {
    if (jvm.instanceOf(testName, "com_codenameone_examples_hellocodenameone_tests_BaseTest")) {
      effectiveTestObject = testName;
    }
  }
  let effectiveTestName = capturedTestName != null ? capturedTestName : testName;
  if (!effectiveTestName || effectiveTestName.__class !== "java_lang_String") {
    if (effectiveTestObject && effectiveTestObject.__class) {
      effectiveTestName = toJavaString(toSimpleClassName(effectiveTestObject.__class));
    } else {
      effectiveTestName = toJavaString("unknown");
    }
  }
  const effectiveIndex = capturedIndex != null ? (capturedIndex | 0) : (index | 0);
  // Prefer runner static test list over lambda captures when available. Captured
  // lambda arguments have been observed to drift after initial indices in ParparVM.
  let indexedTestObject = null;
  if (runner && runner.__class === cn1ssRunnerClassId) {
    try {
      const runnerClass = jvm.classes[cn1ssRunnerClassId];
      const testClasses = runnerClass && runnerClass.staticFields
        ? runnerClass.staticFields["TEST_CLASSES"]
        : null;
      if (testClasses && testClasses.__class) {
        const listGetMethod = jvm.resolveVirtual(testClasses.__class, cn1ssRunnerListGetMethodId);
        indexedTestObject = yield* listGetMethod(testClasses, effectiveIndex);
      }
    } catch (_err) {
      indexedTestObject = null;
    }
  }
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
    emitLambdaBridgeDiag("PARPAR:DIAG:FALLBACK:lambdaBridge:reentry-guard=hit");
    return null;
  }
  if (typeof cn1ssLambdaBridgeOriginalRunnerMethod !== "function") {
    emitLambdaBridgeDiag("PARPAR:DIAG:FALLBACK:lambdaBridge:originalRunnerMethod=missing");
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

const cn1ssChunkIndexByStream = Object.create(null);
const cn1ssScreenshotEmitted = Object.create(null);

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
}

const cn1ssEmitCurrentFormScreenshotMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunnerHelper_emitCurrentFormScreenshot_java_lang_String_java_lang_Runnable";

bindCiFallback("Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshotDom", [
  cn1ssEmitCurrentFormScreenshotMethodId,
  cn1ssEmitCurrentFormScreenshotMethodId + "__impl"
], function*(testName, completion) {
  const canvas = global.document && typeof global.document.querySelector === "function"
    ? global.document.querySelector("canvas")
    : null;
  const test = toCn1StringValue(testName);
  const normalizedTest = normalizeCn1ssTestName(test);
  if (cn1ssScreenshotEmitted[normalizedTest]) {
    emitDiagLine("PARPAR:DIAG:FALLBACK:cn1ssEmitCurrentFormScreenshotDom:skipDuplicate=" + normalizedTest);
  } else if (canvas && typeof canvas.toDataURL === "function") {
    cn1ssScreenshotEmitted[normalizedTest] = true;
    const dataUrl = String(canvas.toDataURL("image/png") || "");
    const comma = dataUrl.indexOf(",");
    const base64 = comma >= 0 ? dataUrl.substring(comma + 1) : "";
    emitCn1ssChunks(base64, normalizedTest, "");
  } else {
    emitDiagLine("PARPAR:DIAG:FALLBACK:cn1ssEmitCurrentFormScreenshotDom:noCanvas=1");
  }
  if (completion && completion.__class) {
    const runMethod = jvm.resolveVirtual(completion.__class, "cn1_java_lang_Runnable_run");
    yield* runMethod(completion);
  }
  return null;
});

bindCiFallback("Cn1ssDeviceRunnerHelper.emitChannelFastJs", [
  cn1ssEmitChannelMethodId,
  cn1ssEmitChannelMethodId + "__impl"
], function*(payloadBytes, testName, channelName) {
  const base64 = byteArrayToBase64(payloadBytes);
  const test = toCn1StringValue(testName);
  const channel = toCn1StringValue(channelName);
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
  emitDiagLine("PARPAR:DIAG:FALLBACK:baseTestRegisterReady:immediateDispatch=1");
  if (!callback || !callback.__class) {
    return null;
  }
  const runMethod = jvm.resolveVirtual(callback.__class, "cn1_java_lang_Runnable_run");
  return yield* runMethod(callback);
});

const baseTestOnShowLambdaMethodId = "cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_1_lambda_onShowCompleted_0_java_lang_String";
const baseTestOnShowLambdaCarrierClass = "com_codenameone_examples_hellocodenameone_tests_BaseTest_1_lambda_0";
if (jvm && typeof jvm.addVirtualMethod === "function" && jvm.classes && jvm.classes[baseTestOnShowLambdaCarrierClass]) {
  const carrierMethods = jvm.classes[baseTestOnShowLambdaCarrierClass].methods || {};
  if (typeof carrierMethods[baseTestOnShowLambdaMethodId] !== "function") {
    jvm.addVirtualMethod(baseTestOnShowLambdaCarrierClass, baseTestOnShowLambdaMethodId, function*(__cn1ThisObject, onShowMessage) {
      const target = __cn1ThisObject
        ? (__cn1ThisObject["cn1_com_codenameone_examples_hellocodenameone_tests_BaseTest_1_lambda_0_arg_1"] || __cn1ThisObject)
        : null;
      if (!target || !target.__class) {
        return null;
      }
      const classDef = target.__classDef;
      let method = (classDef && classDef.methods) ? classDef.methods[baseTestOnShowLambdaMethodId] : null;
      if (!method) {
        method = jvm.resolveVirtual(target.__class, baseTestOnShowLambdaMethodId);
      }
      return yield* method(target, onShowMessage);
    });
    emitDiagLine("PARPAR:DIAG:INIT:shim=baseTestOnShowLambdaDispatch");
  }
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
