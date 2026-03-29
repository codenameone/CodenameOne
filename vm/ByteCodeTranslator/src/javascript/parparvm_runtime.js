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
const jvm = {
  classes: {},
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
    this.classes[className].methods[methodId] = fn;
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
    if (cls.clinit) {
      const gen = cls.clinit();
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
      if (field.desc && field.desc.length && field.desc.charAt(0) !== "L" && field.desc.charAt(0) !== "[") {
        obj[field.prop || (field.owner + "_" + field.name)] = 0;
      }
    }
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
  resolveVirtual(className, methodId) {
    const cacheKey = className + "|" + methodId;
    let cached = this.resolvedVirtualCache[cacheKey];
    if (cached) {
      return cached;
    }
    const tail = this.methodTail(methodId);
    let current = className;
    while (current) {
      const cls = this.classes[current];
      if (cls && cls.methods) {
        if (cls.methods[methodId]) {
          cached = cls.methods[methodId];
          this.resolvedVirtualCache[cacheKey] = cached;
          return cached;
        }
        if (tail) {
          const remappedId = this.remappedMethodId(current, methodId, tail);
          if (cls.methods[remappedId]) {
            cached = cls.methods[remappedId];
            this.resolvedVirtualCache[cacheKey] = cached;
            return cached;
          }
        }
      }
      current = cls ? cls.baseClass : null;
    }
    throw new Error("Missing virtual method " + methodId + " on " + className);
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
  log(message) {
    global.postMessage({ type: this.protocol.messages.LOG, message: message });
  },
  finish(result) {
    global.postMessage({ type: this.protocol.messages.RESULT, result: result });
  },
  fail(error) {
    global.postMessage({ type: this.protocol.messages.ERROR, message: "" + error, stack: error && error.stack ? error.stack : null });
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
  spawn(threadObject, generator) {
    const thread = { id: this.nextThreadId++, object: threadObject, generator: generator, waiting: null, interrupted: false, done: false };
    this.threads.push(thread);
    this.enqueue(thread);
    return thread;
  },
  enqueue(thread, value) {
    thread.waiting = null;
    thread.resumeValue = value;
    this.runnable.push(thread);
    this.drain();
  },
  drain() {
    if (this.draining) {
      return;
    }
    this.draining = true;
    try {
      while (this.runnable.length) {
        const thread = this.runnable.shift();
        if (thread.done) {
          continue;
        }
        this.currentThread = thread;
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
      global.postMessage({ type: this.protocol.messages.HOST_CALL, id: yielded.id, symbol: yielded.symbol, args: yielded.args || [] });
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
  start() {
    if (!this.mainClass || !this.mainMethod) {
      throw new Error("No main class configured for javascript backend");
    }
    const mainArgs = this.newArray(0, "java_lang_String", 1);
    const mainThreadObject = this.newObject("java_lang_Thread");
    mainThreadObject[CN1_THREAD_ALIVE] = 1;
    mainThreadObject[CN1_THREAD_NAME] = this.createStringLiteral("main");
    const mainThread = this.spawn(mainThreadObject, global[this.mainMethod](mainArgs));
    this.currentThread = mainThread;
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
      global.postMessage(this.describeProtocol());
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
function bindNative(names, fn) {
  for (let i = 0; i < names.length; i++) {
    global[names[i]] = jvm[names[i]] = fn;
  }
  return fn;
}
bindNative(["cn1_java_lang_Object_wait_long_int", "cn1_java_lang_Object_wait___long_int"], function*(__cn1ThisObject, timeout, nanos) {
  const resumed = yield jvm.waitOn(jvm.currentThread, __cn1ThisObject, timeout || 0);
  if (resumed && resumed.interrupted) {
    yield* throwInterruptedException();
  }
  return null;
});
bindNative(["cn1_java_lang_Object_notify", "cn1_java_lang_Object_notify__"], function*(__cn1ThisObject) { jvm.notifyOne(__cn1ThisObject); return null; });
bindNative(["cn1_java_lang_Object_notifyAll", "cn1_java_lang_Object_notifyAll__"], function*(__cn1ThisObject) { jvm.notifyAll(__cn1ThisObject); return null; });
bindNative(["cn1_java_lang_Object_hashCode_R_int", "cn1_java_lang_Object_hashCode___R_int"], function*(__cn1ThisObject) { return __cn1ThisObject == null ? 0 : (__cn1ThisObject.__id | 0); });
bindNative(["cn1_java_lang_Object_getClassImpl_R_java_lang_Class", "cn1_java_lang_Object_getClassImpl___R_java_lang_Class"], function*(__cn1ThisObject) { return __cn1ThisObject && __cn1ThisObject.__classDef ? __cn1ThisObject.__classDef.classObject : jvm.getClassObject(__cn1ThisObject.__class); });
bindNative(["cn1_java_lang_Object_toString_R_java_lang_String"], function*(__cn1ThisObject) {
  if (__cn1ThisObject == null) {
    return createJavaString("null");
  }
  return createJavaString(javaClassName(__cn1ThisObject.__class) + "@" + ((__cn1ThisObject.__id | 0).toString(16)));
});
bindNative(["cn1_java_lang_Thread_currentThread_R_java_lang_Thread", "cn1_java_lang_Thread_currentThread___R_java_lang_Thread"], function*() { return jvm.currentThread ? jvm.currentThread.object : null; });
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
bindNative(["cn1_java_lang_System_identityHashCode_java_lang_Object_R_int", "cn1_java_lang_System_identityHashCode___java_lang_Object_R_int"], function*(obj) { return obj == null ? 0 : (obj.__id | 0); });
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
bindNative(["cn1_java_lang_StringBuilder_append_java_lang_Object_R_java_lang_StringBuilder"], function*(__cn1ThisObject, obj) { return sbAppendNativeString(__cn1ThisObject, jvm.toNativeString(obj)); });
bindNative(["cn1_java_lang_StringBuilder_append_java_lang_String_R_java_lang_StringBuilder"], function*(__cn1ThisObject, str) { return sbAppendNativeString(__cn1ThisObject, jvm.toNativeString(str)); });
bindNative(["cn1_java_lang_StringBuilder_charAt_int_R_char"], function*(__cn1ThisObject, index) { return (__cn1ThisObject[CN1_SB_VALUE][index | 0] || 0) | 0; });
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
  let index = 0;
  const values = [];
  if (args && args.__array) {
    for (let i = 0; i < args.length; i++) {
      values.push(yield* runtimeToNativeString(args[i]));
    }
  }
  const result = jvm.toNativeString(format).replace(/%[%sdifc]/g, function(token) {
    if (token === "%%") {
      return "%";
    }
    const value = values[index++];
    if (token === "%c") {
      return String.fromCharCode(value | 0);
    }
    return value;
  });
  return createJavaString(result);
});
bindNative(["cn1_java_lang_StringToReal_parseDblImpl_java_lang_String_int_R_double"], function*(value, exponentIndex) {
  const text = jvm.toNativeString(value);
  const parsed = Number(text);
  return isNaN(parsed) ? 0 : parsed;
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
