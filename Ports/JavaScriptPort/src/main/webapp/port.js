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
          const method = jvm.resolveVirtual(value.__class, "cn1_com_codename1_html5_js_browser_AnimationFrameCallback_onAnimationFrame_double");
          jvm.spawn(null, method(value, +time));
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

bindNative([
  "cn1_com_codename1_impl_html5_HTML5Implementation_requestAnimationFrameNative_com_codename1_impl_html5_JavaScriptAnimationFrameCallback_R_int",
  "cn1_com_codename1_impl_html5_HTML5Implementation_requestAnimationFrameNative___com_codename1_impl_html5_JavaScriptAnimationFrameCallback_R_int"
], function*(handler) {
  const win = global.window || global;
  return (win.requestAnimationFrame || function(cb) { return win.setTimeout(function() { cb(Date.now()); }, 16); })(function(time) {
    try {
      const method = jvm.resolveVirtual(handler.__class, "cn1_com_codename1_impl_html5_JavaScriptAnimationFrameCallback_onAnimationFrame_double");
      jvm.spawn(null, method(handler, +time));
    } catch (err) {
      jvm.fail(err);
    }
  }) |0;
});