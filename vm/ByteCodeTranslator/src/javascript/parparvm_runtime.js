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
const VM_PROTOCOL_VERSION = 1;
const VM_PROTOCOL = Object.freeze({
  version: VM_PROTOCOL_VERSION,
  messages: Object.freeze({
    START: "start",
    EVENT: "event",
    UI_EVENT: "ui-event",
    TIMER_WAKE: "timer-wake",
    HOST_CALL: "host-call",
    HOST_CALLBACK: "host-callback",
    PROTOCOL_INFO: "protocol-info",
    PROTOCOL: "protocol",
    LOG: "log",
    RESULT: "result",
    ERROR: "error"
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
  currentThread: null,
  runnable: [],
  threads: [],
  pendingHostCalls: Object.create(null),
  eventQueue: [],
  mainClass: null,
  mainMethod: null,
  protocol: VM_PROTOCOL,
  diagEnabled: VM_DIAG_ENABLED,
  lastVirtualFailure: null,
  firstFailure: null,
  defineClass(def) {
    def.staticFields = def.staticFields || {};
    def.instanceFields = def.instanceFields || [];
    def.assignableTo = def.assignableTo || {};
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
  },
  addVirtualMethod(className, methodId, fn) {
    const nativeOverride = this.nativeMethods[methodId];
    this.classes[className].methods[methodId] = typeof nativeOverride === "function" ? nativeOverride : fn;
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
      return;
    }
    cls.initializing = true;
    if (cls.baseClass) {
      this.ensureClassInitialized(cls.baseClass);
    }
    cls.initialized = true;
    const clinitMethodId = "cn1_" + className + "___CLINIT__";
    const clinit = this.nativeMethods[clinitMethodId] || cls.clinit;
    if (clinit) {
      const gen = clinit();
      let step = gen.next();
      while (!step.done) {
        if (step.value && (step.value.op === "sleep" || step.value.op === "wait")) {
          throw new Error("Blocking static initializers are not supported in javascript backend");
        }
        step = gen.next();
      }
    }
    cls.initializing = false;
  },
  newObject(className) {
    this.ensureClassInitialized(className);
    const classDef = this.classes[className];
    const obj = { __class: className, __classDef: classDef, __id: this.nextIdentity++, __monitor: this.createMonitor() };
    this.initInstanceFields(obj, className);
    this.initFieldAliases(obj, className);
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
      obj[field.prop || (field.owner + "_" + field.name)] = null;
      if (this.isPrimitiveFieldDescriptor(field.desc)) {
        obj[field.prop || (field.owner + "_" + field.name)] = 0;
      }
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
    const hierarchy = [];
    let current = className;
    while (current) {
      hierarchy.push(current);
      const cls = this.classes[current];
      current = cls ? cls.baseClass : null;
    }
    for (let i = hierarchy.length - 1; i >= 0; i--) {
      const owner = hierarchy[i];
      const cls = this.classes[owner];
      if (!cls || !cls.instanceFields) {
        continue;
      }
      for (let j = 0; j < cls.instanceFields.length; j++) {
        const field = cls.instanceFields[j];
        const canonicalProp = field.prop || this.fieldProperty(field.owner, field.name);
        for (let k = 0; k < i; k++) {
          const aliasProp = this.fieldProperty(hierarchy[k], field.name);
          if (aliasProp === canonicalProp || Object.prototype.hasOwnProperty.call(obj, aliasProp)) {
            continue;
          }
          Object.defineProperty(obj, aliasProp, {
            configurable: true,
            enumerable: false,
            get: function() { return obj[canonicalProp]; },
            set: function(value) { obj[canonicalProp] = value; }
          });
        }
      }
    }
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
      }
      if (cls && cls.methods) {
        if (cls.methods[methodId]) {
          cached = cls.methods[methodId];
          this.resolvedVirtualCache[cacheKey] = cached;
          return cached;
        }
        if (tail) {
          const remappedId = this.remappedMethodId(current, methodId, tail);
          if (remappedId && remappedId !== methodId) {
            remapAttempted = true;
          }
          if (cls.methods[remappedId]) {
            cached = cls.methods[remappedId];
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
      if (iface.methods) {
        if (iface.methods[methodId]) {
          cached = iface.methods[methodId];
          this.resolvedVirtualCache[cacheKey] = cached;
          return cached;
        }
        if (tail) {
          const remappedId = this.remappedMethodId(ifaceName, methodId, tail);
          if (remappedId && remappedId !== methodId) {
            remapAttempted = true;
          }
          if (iface.methods[remappedId]) {
            cached = iface.methods[remappedId];
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
    const self = this;
    return (function*() {
      const receiver = self.unwrapJsValue(__cn1ThisObject);
      if (receiver == null) {
        throw new Error("Null JS interop receiver for " + methodId);
      }
      const bridge = self.parseJsoBridgeMethod(className, methodId);
      const nativeArgs = self.toNativeJsArgs(args || []);
      if (receiver && receiver.__cn1HostRef != null) {
        const transferableArgs = new Array(nativeArgs.length);
        for (let i = 0; i < nativeArgs.length; i++) {
          const arg = nativeArgs[i];
          transferableArgs[i] = (typeof arg === "function") ? null : arg;
        }
        const hostResult = yield self.invokeHostNative("__cn1_jso_bridge__", [{
          receiver: receiver,
          receiverClass: (receiver && receiver.__cn1HostClass) ? receiver.__cn1HostClass : className,
          kind: bridge.kind,
          member: bridge.member,
          args: transferableArgs
        }]);
        return self.wrapJsResult(hostResult, bridge.returnClass);
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
          } else {
            throw new Error("Missing JS member " + bridge.member + " for " + methodId);
          }
        }
      }
      return self.wrapJsResult(result, bridge.returnClass);
    })();
  },
  parseJsoBridgeMethod(className, methodId) {
    const prefix = "cn1_" + className + "_";
    let remainder = methodId.indexOf(prefix) === 0 ? methodId.substring(prefix.length) : methodId;
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
    if (member.indexOf("set") === 0 && member.length > 3 && remainder.indexOf("_") > -1 && member !== "setAttribute" && member !== "setProperty") {
      return { kind: "setter", member: lowerFirst(member.substring(3)), returnClass: returnClass };
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
    return !!(obj && obj.__classDef && obj.__classDef.assignableTo && obj.__classDef.assignableTo[className]);
  },
  findExceptionHandler(entries, pc, error) {
    if (!entries || !entries.length) {
      return null;
    }
    const errorClass = error == null ? null : error.__class;
    const errorClassDef = error == null ? null : error.__classDef;
    for (let i = 0; i < entries.length; i++) {
      const entry = entries[i];
      if (pc < entry.start || pc >= entry.end) {
        continue;
      }
      if (entry.type == null) {
        return entry;
      }
      if (errorClass === entry.type || (errorClassDef && errorClassDef.assignableTo && errorClassDef.assignableTo[entry.type])) {
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
    return value && value.__jsValue !== undefined ? value.__jsValue : value;
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
      wrapper.__class = resolvedClass;
      this.enhanceJsWrapper(wrapper, resolvedClass);
      if (expectedClass && expectedClass !== resolvedClass) {
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
    const message = "" + error;
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
    emitVmMessage({
      type: this.protocol.messages.ERROR,
      message: message,
      stack: error && error.stack ? error.stack : null,
      virtualFailure: virtualFailure || null
    });
  },
  invokeHostNative(symbol, args) {
    return { op: this.protocol.messages.HOST_CALL, id: this.nextHostCallId++, symbol: symbol, args: args || [] };
  },
  resolveHostCall(id, success, value, error) {
    const pending = this.pendingHostCalls[id];
    if (!pending) {
      return false;
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
  toHostTransferArg(value) {
    if (value == null) {
      return value;
    }
    const type = typeof value;
    if (type === "string" || type === "number" || type === "boolean") {
      return value;
    }
    if (type === "function") {
      return null;
    }
    if (Array.isArray(value)) {
      const out = new Array(value.length);
      for (let i = 0; i < value.length; i++) {
        out[i] = this.toHostTransferArg(value[i]);
      }
      return out;
    }
    if (value.__cn1HostRef != null) {
      return value.__cn1HostClass
        ? { __cn1HostRef: value.__cn1HostRef, __cn1HostClass: value.__cn1HostClass }
        : { __cn1HostRef: value.__cn1HostRef };
    }
    if (value.__jsValue !== undefined) {
      return this.toHostTransferArg(value.__jsValue);
    }
    if (type === "object") {
      const out = {};
      const keys = Object.keys(value);
      for (let i = 0; i < keys.length; i++) {
        const key = keys[i];
        out[key] = this.toHostTransferArg(value[key]);
      }
      return out;
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
    this.enqueue(thread);
    return thread;
  },
  enqueue(thread, value) {
    thread.waiting = null;
    thread.resumeValue = value;
    this.runnable.push(thread);
    this.drain();
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
    this.draining = true;
    const deadline = this.schedulerNow() + 8;
    let steps = 0;
    try {
      while (this.runnable.length) {
        if (steps++ > 2048 || this.schedulerNow() >= deadline) {
          this.scheduleDrain();
          break;
        }
        const thread = this.runnable.shift();
        if (thread.done) {
          continue;
        }
        this.currentThread = thread;
        if (!thread.__cn1LoggedFirstStep && shouldTraceThread(thread)) {
          thread.__cn1LoggedFirstStep = true;
          vmTrace("runtime.drain.first-step.thread-" + thread.id + ":" + threadDebugLabel(thread.object));
        }
        let result;
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
          if (thread.object) {
            thread.object[CN1_THREAD_ALIVE] = 0;
            this.notifyAll(thread.object);
          }
          continue;
        }
        this.handleYield(thread, result.value);
      }
    } catch (err) {
      this.fail(err);
    } finally {
      this.currentThread = null;
      this.draining = false;
    }
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
    if (yielded.op === "sleep") {
      const timer = setTimeout(() => this.enqueue(thread), Math.max(0, yielded.millis | 0));
      thread.waiting = { op: "sleep", timer: timer };
      return;
    }
    if (yielded.op === "wait") {
      const waiter = { thread: thread, monitor: yielded.monitor, reentryCount: yielded.reentryCount };
      yielded.monitor.__monitor.waiters.push(waiter);
      if (yielded.timeout > 0) {
        waiter.timer = setTimeout(() => this.resumeWaiter(waiter), yielded.timeout);
      }
      thread.waiting = { op: "wait", waiter: waiter };
      return;
    }
    if (yielded.op === this.protocol.messages.HOST_CALL) {
      thread.waiting = { op: this.protocol.messages.HOST_CALL, id: yielded.id };
      this.pendingHostCalls[yielded.id] = { thread: thread };
      const rawArgs = yielded.args || [];
      const safeArgs = new Array(rawArgs.length);
      for (let i = 0; i < rawArgs.length; i++) {
        safeArgs[i] = this.toHostTransferArg(rawArgs[i]);
      }
      emitVmMessage({ type: this.protocol.messages.HOST_CALL, id: yielded.id, symbol: yielded.symbol, args: safeArgs });
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
      return;
    }
    throw new Error("Blocking monitor acquisition is not yet supported in javascript backend");
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
    return { op: "wait", monitor: obj, timeout: timeout | 0, reentryCount: reentryCount };
  },
  notifyOne(obj) {
    const monitor = obj.__monitor || (obj.__monitor = this.createMonitor());
    const waiter = monitor.waiters.shift();
    if (!waiter) {
      return;
    }
    if (waiter.timer) {
      clearTimeout(waiter.timer);
    }
    this.resumeWaiter(waiter);
  },
  notifyAll(obj) {
    const monitor = obj.__monitor || (obj.__monitor = this.createMonitor());
    const waiters = monitor.waiters.splice(0, monitor.waiters.length);
    for (const waiter of waiters) {
      if (waiter.timer) {
        clearTimeout(waiter.timer);
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
      clearTimeout(thread.waiting.timer);
      this.enqueue(thread, { interrupted: true });
      return;
    }
    if (thread.waiting.op === "wait") {
      const waiter = thread.waiting.waiter;
      if (waiter.timer) {
        clearTimeout(waiter.timer);
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
    const ctor = global["cn1_" + className + "___INIT__"];
    return { object: ex, ctor: ctor };
  },
  applyNativeOverrides() {
    const classNames = Object.keys(this.classes);
    for (let i = 0; i < classNames.length; i++) {
      const cls = this.classes[classNames[i]];
      if (!cls || !cls.methods) {
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
      throw new Error("No main class configured for javascript backend");
    }
    vmDiag("LIFECYCLE_START", "mainClass", this.mainClass);
    this.applyNativeOverrides();
    ensureSystemPrintStreams();
    vmTrace("runtime.start.before-main-generator");
    const mainArgs = this.newArray(0, "java_lang_String", 1);
    const mainThreadObject = this.newObject("java_lang_Thread");
    mainThreadObject[CN1_THREAD_ALIVE] = 1;
    mainThreadObject[CN1_THREAD_NAME] = this.createStringLiteral("main");
    this.mainThreadObject = mainThreadObject;
    const mainGenerator = global[this.mainMethod](mainArgs);
    vmTrace("runtime.start.after-main-generator");
    const mainThread = this.spawn(mainThreadObject, mainGenerator);
    vmTrace("runtime.start.after-spawn");
    this.currentThread = mainThread;
    vmTrace("runtime.start.before-drain");
    this.drain();
    vmTrace("runtime.start.after-drain");
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
    return false;
  }
};

global.jvm = jvm;
jvm.jsoRegistry = jsoRegistry;
global.bindNative = bindNative;
global.global = global;
global.__parparInstallNativeBindings = installNativeBindings;
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
    const toStringMethod = jvm.resolveVirtual(value.__class, "cn1_java_lang_Object_toString_R_java_lang_String");
    return jvm.toNativeString(yield* toStringMethod(value));
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
    if (primitive != null) {
      return String(Math.trunc(Number(primitive)));
    }
    return String(Math.trunc(Number(value == null ? 0 : value)));
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
  view.setFloat64(0, value, false);
  const hi = view.getUint32(0, false);
  const lo = view.getUint32(4, false);
  return (hi * 4294967296) + lo;
}
function doubleFromLongBits(bits) {
  const hi = Math.floor(bits / 4294967296);
  const lo = bits >>> 0;
  const view = new DataView(new ArrayBuffer(8));
  view.setUint32(0, hi >>> 0, false);
  view.setUint32(4, lo, false);
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
    yield* ex.ctor(ex.object);
  }
  throw ex.object;
}
function* throwNullPointerException() {
  const ex = jvm.createException("java_lang_NullPointerException");
  if (typeof ex.ctor === "function") {
    yield* ex.ctor(ex.object);
  }
  throw ex.object;
}
function bindNative(names, fn) {
  function installVirtualOverride(name) {
    const classes = jvm.classes || {};
    const classNames = Object.keys(classes);
    for (let i = 0; i < classNames.length; i++) {
      const cls = classes[classNames[i]];
      if (!cls || !cls.methods || !Object.prototype.hasOwnProperty.call(cls.methods, name)) {
        continue;
      }
      cls.methods[name] = fn;
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
    }
  }
}
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
bindNative(["cn1_org_teavm_classlib_impl_tz_DateTimeZoneProvider_timeZoneDetectionEnabled_R_boolean", "cn1_org_teavm_classlib_impl_tz_DateTimeZoneProvider_timeZoneDetectionEnabled___R_boolean"], function*() {
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
    const equalsMethod = jvm.resolveVirtual(a.__class, "cn1_java_lang_Object_equals_java_lang_Object_R_boolean");
    return yield* equalsMethod(a, b);
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
bindNative(["cn1_java_lang_Object_notify", "cn1_java_lang_Object_notify__"], function*(__cn1ThisObject) { jvm.notifyOne(__cn1ThisObject); return null; });
bindNative(["cn1_java_lang_Object_notifyAll", "cn1_java_lang_Object_notifyAll__"], function*(__cn1ThisObject) { jvm.notifyAll(__cn1ThisObject); return null; });
bindNative(["cn1_java_lang_Object_hashCode_R_int", "cn1_java_lang_Object_hashCode___R_int"], function*(__cn1ThisObject) { return __cn1ThisObject == null ? 0 : (__cn1ThisObject.__id | 0); });
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
bindNative(["cn1_java_io_PrintStream_print_java_lang_String"], function*(__cn1ThisObject, value) {
  printToConsole(printStreamValue(value));
  return null;
});
bindNative(["cn1_java_io_PrintStream_println_java_lang_String"], function*(__cn1ThisObject, value) {
  printToConsole(printStreamValue(value));
  return null;
});
bindNative(["cn1_java_io_PrintStream_println_java_lang_Object"], function*(__cn1ThisObject, value) {
  printToConsole(printStreamValue(value));
  return null;
});
bindNative(["cn1_java_lang_System___CLINIT__"], function*() {
  ensureSystemPrintStreams();
  return null;
});
bindNative(["cn1_java_lang_Object_toString_R_java_lang_String"], function*(__cn1ThisObject) {
  if (__cn1ThisObject == null) {
    return createJavaString("null");
  }
  return createJavaString(javaClassName(__cn1ThisObject.__class) + "@" + ((__cn1ThisObject.__id | 0).toString(16)));
});
bindNative(["cn1_java_lang_Thread_currentThread_R_java_lang_Thread", "cn1_java_lang_Thread_currentThread___R_java_lang_Thread"], function*() {
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
bindNative(["cn1_java_lang_Thread_setPriorityImpl_int", "cn1_java_lang_Thread_setPriorityImpl___int"], function*() { return null; });
bindNative(["cn1_java_lang_Thread_interrupt0", "cn1_java_lang_Thread_interrupt0__"], function*(__cn1ThisObject) { jvm.interruptThread(__cn1ThisObject); return null; });
bindNative(["cn1_java_lang_Thread_isInterrupted_boolean_R_boolean", "cn1_java_lang_Thread_isInterrupted___boolean_R_boolean"], function*(__cn1ThisObject, clearInterrupted) { const value = __cn1ThisObject && __cn1ThisObject.__interrupted ? 1 : 0; if (clearInterrupted && __cn1ThisObject) __cn1ThisObject.__interrupted = 0; return value; });
bindNative(["cn1_java_lang_Thread_getNativeThreadId_R_long", "cn1_java_lang_Thread_getNativeThreadId___R_long"], function*() { return jvm.currentThread ? jvm.currentThread.id : 0; });
bindNative(["cn1_java_lang_Thread_releaseThreadNativeResources_long", "cn1_java_lang_Thread_releaseThreadNativeResources___long"], function*() { return null; });
bindNative(["cn1_java_lang_Thread_start", "cn1_java_lang_Thread_start__"], function*(__cn1ThisObject) {
  const tid = jvm.nextThreadId;
  __cn1ThisObject[CN1_THREAD_ALIVE] = 1;
  __cn1ThisObject[CN1_THREAD_NATIVE_ID] = tid;
  jvm.classes["java_lang_Thread"].staticFields["activeThreads"] = ((jvm.classes["java_lang_Thread"].staticFields["activeThreads"] | 0) + 1) | 0;
  const target = __cn1ThisObject[CN1_THREAD_TARGET] || __cn1ThisObject;
  const generator = (function*() {
    try {
      const runMethod = jvm.resolveVirtual(target.__class, "cn1_java_lang_Runnable_run");
      yield* runMethod(target);
    } catch (err) {
      jvm.fail(err);
    } finally {
      jvm.classes["java_lang_Thread"].staticFields["activeThreads"] = ((jvm.classes["java_lang_Thread"].staticFields["activeThreads"] | 0) - 1) | 0;
    }
  })();
  jvm.spawn(__cn1ThisObject, generator);
  return null;
});
bindNative(["cn1_java_lang_System_currentTimeMillis_R_long", "cn1_java_lang_System_currentTimeMillis___R_long"], function*() { return Date.now(); });
bindNative(["cn1_java_lang_System_identityHashCode_java_lang_Object_R_int", "cn1_java_lang_System_identityHashCode___java_lang_Object_R_int"], function*(obj) { return identityHash(obj); });
bindNative(["cn1_java_lang_System_arraycopy_java_lang_Object_int_java_lang_Object_int_int", "cn1_java_lang_System_arraycopy___java_lang_Object_int_java_lang_Object_int_int"], function*(src, srcOffset, dst, dstOffset, length) {
  for (let i = 0; i < length; i++) dst[dstOffset + i] = src[srcOffset + i];
  return null;
});
bindNative(["cn1_java_lang_System_gcLight", "cn1_java_lang_System_gcLight__"], function*() { return null; });
bindNative(["cn1_java_lang_System_gcMarkSweep", "cn1_java_lang_System_gcMarkSweep__"], function*() { return null; });
bindNative(["cn1_java_lang_System_isHighFrequencyGC_R_boolean", "cn1_java_lang_System_isHighFrequencyGC___R_boolean"], function*() { return 0; });
bindNative(["cn1_java_lang_System_exit_int", "cn1_java_lang_System_exit___int"], function*(status) { jvm.finish(status); return null; });
bindNative(["cn1_java_lang_Runtime_totalMemoryImpl_R_long"], function*() { return 67108864; });
bindNative(["cn1_java_lang_Runtime_freeMemoryImpl_R_long"], function*() { return 33554432; });
bindNative(["cn1_java_lang_Throwable_fillInStack"], function*(__cn1ThisObject) { __cn1ThisObject[CN1_THROWABLE_STACK] = createJavaString(new Error().stack || ""); return null; });
bindNative(["cn1_java_lang_Throwable_getStack_R_java_lang_String"], function*(__cn1ThisObject) { return __cn1ThisObject[CN1_THROWABLE_STACK] || createJavaString(""); });
bindNative(["cn1_java_lang_Math_abs_double_R_double"], function*(v) { return Math.abs(v); });
bindNative(["cn1_java_lang_Math_abs_float_R_float"], function*(v) { return Math.abs(v); });
bindNative(["cn1_java_lang_Math_abs_int_R_int"], function*(v) { return Math.abs(v | 0); });
bindNative(["cn1_java_lang_Math_abs_long_R_long"], function*(v) { return Math.abs(v); });
bindNative(["cn1_java_lang_Math_ceil_double_R_double"], function*(v) { return Math.ceil(v); });
bindNative(["cn1_java_lang_Math_floor_double_R_double"], function*(v) { return Math.floor(v); });
bindNative(["cn1_java_lang_Math_max_double_double_R_double"], function*(a, b) { return Math.max(a, b); });
bindNative(["cn1_java_lang_Math_max_float_float_R_float"], function*(a, b) { return Math.max(a, b); });
bindNative(["cn1_java_lang_Math_max_int_int_R_int"], function*(a, b) { return Math.max(a | 0, b | 0); });
bindNative(["cn1_java_lang_Math_max_long_long_R_long"], function*(a, b) { return Math.max(a, b); });
bindNative(["cn1_java_lang_Math_min_double_double_R_double"], function*(a, b) { return Math.min(a, b); });
bindNative(["cn1_java_lang_Math_min_float_float_R_float"], function*(a, b) { return Math.min(a, b); });
bindNative(["cn1_java_lang_Math_min_int_int_R_int"], function*(a, b) { return Math.min(a | 0, b | 0); });
bindNative(["cn1_java_lang_Math_min_long_long_R_long"], function*(a, b) { return Math.min(a, b); });
bindNative(["cn1_java_lang_Math_pow_double_double_R_double"], function*(a, b) { return Math.pow(a, b); });
bindNative(["cn1_java_lang_Math_cos_double_R_double"], function*(v) { return Math.cos(v); });
bindNative(["cn1_java_lang_Math_sin_double_R_double"], function*(v) { return Math.sin(v); });
bindNative(["cn1_java_lang_Math_sqrt_double_R_double"], function*(v) { return Math.sqrt(v); });
bindNative(["cn1_java_lang_Math_tan_double_R_double"], function*(v) { return Math.tan(v); });
bindNative(["cn1_java_lang_Math_atan_double_R_double"], function*(v) { return Math.atan(v); });
bindNative(["cn1_java_lang_Integer_toString_int_R_java_lang_String"], function*(v) { return createJavaString(String(v | 0)); });
bindNative(["cn1_java_lang_Integer_toString_int_int_R_java_lang_String"], function*(v, radix) { return createJavaString((v | 0).toString((radix | 0) || 10)); });
bindNative(["cn1_java_lang_Long_toString_long_int_R_java_lang_String"], function*(v, radix) { return createJavaString(Math.trunc(v).toString((radix | 0) || 10)); });
bindNative(["cn1_java_lang_Character_toLowerCase_char_R_char"], function*(ch) { return String.fromCharCode(ch | 0).toLowerCase().charCodeAt(0) | 0; });
bindNative(["cn1_java_lang_Character_toLowerCase_int_R_int"], function*(ch) { return String.fromCharCode(ch | 0).toLowerCase().charCodeAt(0) | 0; });
bindNative(["cn1_java_lang_Float_floatToIntBits_float_R_int"], function*(v) { return intBitsFromFloat(v); });
bindNative(["cn1_java_lang_Float_intBitsToFloat_int_R_float"], function*(bits) { return floatFromIntBits(bits); });
bindNative(["cn1_java_lang_Float_toStringImpl_float_boolean_R_java_lang_String"], function*(v) { return createJavaString(String(v)); });
bindNative(["cn1_java_lang_Double_doubleToLongBits_double_R_long"], function*(v) { return longBitsFromDouble(v); });
bindNative(["cn1_java_lang_Double_longBitsToDouble_long_R_double"], function*(bits) { return doubleFromLongBits(bits); });
bindNative(["cn1_java_lang_Double_toStringImpl_double_boolean_R_java_lang_String"], function*(v) { return createJavaString(String(v)); });
bindNative(["cn1_java_lang_StringBuilder_append_char_R_java_lang_StringBuilder"], function*(__cn1ThisObject, ch) { return sbAppendNativeString(__cn1ThisObject, String.fromCharCode(ch | 0)); });
bindNative(["cn1_java_lang_StringBuilder_append_int_R_java_lang_StringBuilder"], function*(__cn1ThisObject, value) { return sbAppendNativeString(__cn1ThisObject, String(value | 0)); });
bindNative(["cn1_java_lang_StringBuilder_append_long_R_java_lang_StringBuilder"], function*(__cn1ThisObject, value) { return sbAppendNativeString(__cn1ThisObject, String(Math.trunc(value))); });
bindNative(["cn1_java_lang_StringBuilder_append_java_lang_Object_R_java_lang_StringBuilder"], function*(__cn1ThisObject, obj) { return sbAppendNativeString(__cn1ThisObject, jvm.toNativeString(obj)); });
bindNative(["cn1_java_lang_StringBuilder_append_java_lang_String_R_java_lang_StringBuilder"], function*(__cn1ThisObject, str) { return sbAppendNativeString(__cn1ThisObject, jvm.toNativeString(str)); });
bindNative(["cn1_java_lang_StringBuilder_charAt_int_R_char"], function*(__cn1ThisObject, index) { return (__cn1ThisObject[CN1_SB_VALUE][index | 0] || 0) | 0; });
bindNative(["cn1_java_lang_StringBuilder_length_R_int"], function*(__cn1ThisObject) { return __cn1ThisObject[CN1_SB_COUNT] | 0; });
bindNative(["cn1_java_lang_StringBuilder_toString_R_java_lang_String"], function*(__cn1ThisObject) {
  const count = __cn1ThisObject[CN1_SB_COUNT] | 0;
  const data = __cn1ThisObject[CN1_SB_VALUE];
  let out = "";
  for (let i = 0; i < count; i++) {
    out += String.fromCharCode(data[i] | 0);
  }
  return createJavaString(out);
});
bindNative(["cn1_java_lang_StringBuilder_getChars_int_int_char_1ARRAY_int"], function*(__cn1ThisObject, start, end, dst, dstStart) {
  const value = __cn1ThisObject[CN1_SB_VALUE];
  for (let i = start | 0; i < (end | 0); i++) {
    dst[(dstStart | 0) + i - (start | 0)] = value[i] | 0;
  }
  return null;
});
bindNative(["cn1_java_lang_String_charAt_int_R_char"], function*(__cn1ThisObject, index) { return jvm.toNativeString(__cn1ThisObject).charCodeAt(index | 0) | 0; });
bindNative(["cn1_java_lang_String_equals_java_lang_Object_R_boolean"], function*(__cn1ThisObject, obj) {
  return (obj != null && obj.__class === "java_lang_String" && jvm.toNativeString(__cn1ThisObject) === jvm.toNativeString(obj)) ? 1 : 0;
});
bindNative(["cn1_java_lang_String_equalsIgnoreCase_java_lang_String_R_boolean"], function*(__cn1ThisObject, other) {
  return (other != null && jvm.toNativeString(__cn1ThisObject).toLowerCase() === jvm.toNativeString(other).toLowerCase()) ? 1 : 0;
});
bindNative(["cn1_java_lang_String_getChars_int_int_char_1ARRAY_int"], function*(__cn1ThisObject, start, end, dst, dstStart) {
  const value = jvm.toNativeString(__cn1ThisObject);
  for (let i = start | 0; i < (end | 0); i++) {
    dst[(dstStart | 0) + i - (start | 0)] = value.charCodeAt(i) | 0;
  }
  return null;
});
bindNative(["cn1_java_lang_String_hashCode_R_int"], function*(__cn1ThisObject) {
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
bindNative(["cn1_java_lang_String_indexOf_int_int_R_int"], function*(__cn1ThisObject, ch, fromIndex) { return jvm.toNativeString(__cn1ThisObject).indexOf(String.fromCharCode(ch | 0), fromIndex | 0); });
bindNative(["cn1_java_lang_String_toLowerCase_R_java_lang_String"], function*(__cn1ThisObject) { return createJavaString(jvm.toNativeString(__cn1ThisObject).toLowerCase()); });
bindNative(["cn1_java_lang_String_toString_R_java_lang_String"], function*(__cn1ThisObject) { return __cn1ThisObject; });
bindNative(["cn1_java_lang_String_toUpperCase_R_java_lang_String"], function*(__cn1ThisObject) { return createJavaString(jvm.toNativeString(__cn1ThisObject).toUpperCase()); });
bindNative(["cn1_java_lang_String_releaseNSString_long"], function*() { return null; });
bindNative(["cn1_java_lang_String_bytesToChars_byte_1ARRAY_int_int_java_lang_String_R_char_1ARRAY"], function*(bytes, off, len, encoding) {
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
  return yield* cn1_java_lang_String_bytesToChars_byte_1ARRAY_int_int_java_lang_String_R_char_1ARRAY(bytes, off, len, encoding);
});
bindNative(["cn1_java_lang_String_charsToBytes_char_1ARRAY_char_1ARRAY_R_byte_1ARRAY"], function*(chars) {
  let text = "";
  for (let i = 0; i < chars.length; i++) {
    text += String.fromCharCode(chars[i] | 0);
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
bindNative(["cn1_java_lang_StringToReal_parseDblImpl_java_lang_String_int_R_double"], function*(value, exponentIndex) {
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
bindNative(["cn1_java_lang_Enum_valueOf_java_lang_Class_java_lang_String_R_java_lang_Enum"], function*(enumType, name) {
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
bindNative(["cn1_java_lang_Class_forNameImpl_java_lang_String_R_java_lang_Class"], function*(className) {
  const runtimeName = runtimeTypeFromJavaName(jvm.toNativeString(className));
  const cls = jvm.classes[runtimeName];
  return cls ? cls.classObject : null;
});
bindNative(["cn1_java_lang_Class_getNameImpl_R_java_lang_String"], function*(__cn1ThisObject) {
  return createJavaString(javaClassName(__cn1ThisObject.__classDef.name));
});
bindNative(["cn1_java_lang_Class_getName_R_java_lang_String"], function*(__cn1ThisObject) { return createJavaString(descriptorClassName(__cn1ThisObject.__classDef.name)); });
bindNative(["cn1_java_lang_Class_isArray_R_boolean"], function*(__cn1ThisObject) { return __cn1ThisObject.__classDef && __cn1ThisObject.__classDef.name.indexOf("[]") > -1 ? 1 : 0; });
bindNative(["cn1_java_lang_Class_isAssignableFrom_java_lang_Class_R_boolean"], function*(__cn1ThisObject, cls) { return cls && cls.__classDef && cls.__classDef.assignableTo[__cn1ThisObject.__classDef.name] ? 1 : 0; });
bindNative(["cn1_java_lang_Class_isInstance_java_lang_Object_R_boolean"], function*(__cn1ThisObject, obj) { return jvm.instanceOf(obj, __cn1ThisObject.__classDef.name) ? 1 : 0; });
bindNative(["cn1_java_lang_Class_isInterface_R_boolean"], function*(__cn1ThisObject) { return __cn1ThisObject.__classDef && __cn1ThisObject.__classDef.isInterface ? 1 : 0; });
bindNative(["cn1_java_lang_Class_newInstanceImpl_R_java_lang_Object"], function*(__cn1ThisObject) {
  const def = __cn1ThisObject.__classDef;
  if (!def || def.isInterface || def.isAbstract || def.isPrimitive || def.name.indexOf("[]") > -1) {
    return null;
  }
  const obj = jvm.newObject(def.name);
  const ctor = global["cn1_" + def.name + "___INIT__"];
  if (typeof ctor === "function") {
    yield* ctor(obj);
  }
  return obj;
});
bindNative(["cn1_java_lang_Class_isAnnotation_R_boolean"], function*() { return 0; });
bindNative(["cn1_java_lang_Class_isEnum_R_boolean"], function*() { return 0; });
bindNative(["cn1_java_lang_Class_isAnonymousClass_R_boolean"], function*() { return 0; });
bindNative(["cn1_java_lang_Class_isSynthetic_R_boolean"], function*() { return 0; });
bindNative(["cn1_java_lang_Class_hashCode_R_int"], function*(__cn1ThisObject) { return __cn1ThisObject && __cn1ThisObject.__classDef ? (__cn1ThisObject.__classDef.name.length | 0) : 0; });
bindNative(["cn1_java_lang_Class_getComponentType_R_java_lang_Class"], function*(__cn1ThisObject) {
  const def = __cn1ThisObject.__classDef;
  if (!def || def.name.indexOf("[]") < 0) {
    return null;
  }
  return classObjectForName(def.componentClass);
});
bindNative(["cn1_java_lang_Class_isPrimitive_R_boolean"], function*(__cn1ThisObject) { return __cn1ThisObject.__classDef && __cn1ThisObject.__classDef.isPrimitive ? 1 : 0; });
bindNative(["cn1_java_lang_reflect_Array_newInstanceImpl_java_lang_Class_int_R_java_lang_Object"], function*(componentClass, length) {
  if (!componentClass || !componentClass.__classDef) {
    return null;
  }
  return jvm.newArray(length | 0, componentClass.__classDef.name, 1);
});
bindNative(["cn1_java_util_Locale_getOSLanguage_R_java_lang_String"], function*() {
  let locale = null;
  if (typeof navigator !== "undefined" && navigator.language) {
    locale = navigator.language;
  } else if (typeof Intl !== "undefined" && Intl.DateTimeFormat) {
    locale = Intl.DateTimeFormat().resolvedOptions().locale;
  }
  return createJavaString(locale || "en-US");
});
bindNative(["cn1_java_util_TimeZone_getTimezoneId_R_java_lang_String"], function*() {
  return createJavaString(defaultTimeZoneId());
});
bindNative(["cn1_java_util_TimeZone_getTimezoneOffset_java_lang_String_int_int_int_int_R_int"], function*(name, year, month, day, timeOfDayMillis) {
  const tz = normalizeTimeZoneId(name);
  const millis = Date.UTC((year | 0), ((month | 0) - 1), day | 0, 0, 0, 0, 0) + (timeOfDayMillis | 0);
  return timezoneOffsetMillis(tz, millis);
});
bindNative(["cn1_java_util_TimeZone_getTimezoneRawOffset_java_lang_String_R_int"], function*(name) {
  return timezoneRawOffsetMillis(normalizeTimeZoneId(name));
});
bindNative(["cn1_java_util_TimeZone_isTimezoneDST_java_lang_String_long_R_boolean"], function*(name, millis) {
  const tz = normalizeTimeZoneId(name);
  return timezoneOffsetMillis(tz, millis) !== timezoneRawOffsetMillis(tz) ? 1 : 0;
});
bindNative(["cn1_java_text_DateFormat_format_java_util_Date_java_lang_StringBuffer_R_java_lang_String"], function*(__cn1ThisObject, date, toAppendTo) {
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
  const equalsMethod = jvm.resolveVirtual(key1.__class, "cn1_java_lang_Object_equals_java_lang_Object_R_boolean");
  return (yield* equalsMethod(key1, key2)) ? 1 : 0;
});
bindNative(["cn1_java_util_HashMap_findNonNullKeyEntry_java_lang_Object_int_int_R_java_util_HashMap_Entry"], function*(__cn1ThisObject, key, index, keyHash) {
  const buckets = __cn1ThisObject[CN1_HASHMAP_ELEMENT_DATA];
  let entry = buckets == null ? null : buckets[index | 0];
  while (entry != null) {
    if (((entry.cn1_java_util_HashMap_Entry_origKeyHash | 0) === (keyHash | 0))
            && (yield* cn1_java_util_HashMap_areEqualKeys_java_lang_Object_java_lang_Object_R_boolean(key, entry[CN1_HASHMAP_ENTRY_KEY]))) {
      return entry;
    }
    entry = entry[CN1_HASHMAP_ENTRY_NEXT];
  }
  return null;
});
bindNative(["cn1_java_util_LinkedHashMap_findNonNullKeyEntry_java_lang_Object_int_int_R_java_util_HashMap_Entry"], function*(__cn1ThisObject, key, index, keyHash) {
  return yield* cn1_java_util_HashMap_findNonNullKeyEntry_java_lang_Object_int_int_R_java_util_HashMap_Entry(__cn1ThisObject, key, index, keyHash);
});
bindNative(["cn1_java_io_NSLogOutputStream_write_byte_1ARRAY_int_int"], function*(__cn1ThisObject, bytes, off, len) {
  const chars = yield* cn1_java_lang_String_bytesToChars_byte_1ARRAY_int_int_java_lang_String_R_char_1ARRAY(bytes, off, len, createJavaString("utf-8"));
  jvm.log(nativeStringFromCharArray(chars));
  return null;
});
bindNative(["cn1_com_codename1_impl_platform_js_VMHost_getLastEventCode_R_int",
            "cn1_com_codename1_impl_platform_js_VMHost_getLastEventCode___R_int"], function*() {
  if (!jvm.lastEvent || jvm.lastEvent.code == null) {
    return -1;
  }
  return jvm.lastEvent.code | 0;
});
bindNative(["cn1_com_codename1_impl_platform_js_VMHost_pollEventCode_R_int",
            "cn1_com_codename1_impl_platform_js_VMHost_pollEventCode___R_int"], function*() {
  if (!jvm.eventQueue.length) {
    return -1;
  }
  const event = jvm.eventQueue.shift();
  return event && event.code != null ? (event.code | 0) : -1;
});
})(self);
