/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

// Runs a program the ParparVM JavaScript target produced -- the self-hosted
// translator, above all -- as a command-line program under Node:
//
//   node run-program.js <bundle-dir> [program arguments...]
//
// With CN1_PRELOAD_JS=<file.js>[:<file.js>...] those scripts are evaluated in the
// VM's global scope after the bundle loads and before main runs.
//
// The bundle is loaded the way worker.js loads it (runtime first, then every
// translated_app chunk in order), main(String[]) runs on the VM's cooperative
// scheduler, and the process exits with the program's System.exit status, or 0
// when main returns. java.io works on the real file system: the runtime's file
// natives are handed a jvm.fileSystem backed by fs, resolving relative paths
// against the current directory. The process environment is passed through, so
// CN1_RESOURCE_PATH and the translator's other knobs work as they do natively.
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const bundle = process.argv[2];
if (!bundle) {
  console.error('usage: node run-program.js <bundle-dir> [arguments...]');
  process.exit(2);
}
const programArgs = process.argv.slice(3);

global.self = global;
global.window = global;
let exitStatus = null;
global.postMessage = function(message) {
  if (!message || typeof message !== 'object') {
    return;
  }
  if (message.type === 'result' && exitStatus === null) {
    exitStatus = message.result | 0;
  } else if (message.type === 'error') {
    console.error('VM error: ' + (message.error || JSON.stringify(message)));
    exitStatus = 1;
  } else if (message.type === 'log' && message.message != null) {
    process.stdout.write(String(message.message) + '\n');
  }
};
// Timers run in virtual time, in due order, once nothing is runnable: a
// command-line program has no event loop of its own to wait on.
let timerSeq = 1;
let virtualNow = Date.now();
const timers = [];
global.setTimeout = function(fn, millis) {
  const timer = { id: timerSeq++, due: virtualNow + Math.max(0, millis | 0), fn: fn, cleared: false };
  timers.push(timer);
  return timer;
};
global.clearTimeout = function(timer) {
  if (timer) {
    timer.cleared = true;
  }
};

function load(file) {
  const full = path.join(bundle, file);
  let src = fs.readFileSync(full, 'utf8');
  if (file === 'translated_app.js') {
    src += '\nif (typeof jvm !== "undefined" && jvm.mainMethod) { global.__cn1ExportedMain = eval(jvm.mainMethod); }\n';
  }
  vm.runInThisContext(src, { filename: full });
}

load('parparvm_runtime.js');
const chunks = fs.readdirSync(bundle).filter(function(name) {
  return /^translated_app_\d+\.js$/.test(name);
}).sort(function(a, b) {
  return parseInt(a.substring(15), 10) - parseInt(b.substring(15), 10);
});
for (const chunk of chunks) {
  load(chunk);
}
load('translated_app.js');
// As worker.js does: the translated chunks define their own stubs, so the
// runtime's native bindings are applied again on top of them.
if (typeof global.__parparInstallNativeBindings === 'function') {
  global.__parparInstallNativeBindings();
}

jvm.env = Object.assign({}, process.env);
jvm.fileSystem = {
  resolve(p) {
    return path.resolve(p);
  },
  stat(abs) {
    try {
      const st = fs.statSync(abs);
      return { dir: st.isDirectory(), size: st.size, mtime: st.mtimeMs };
    } catch (e) {
      return null;
    }
  },
  list(abs) {
    try {
      return fs.statSync(abs).isDirectory() ? fs.readdirSync(abs) : null;
    } catch (e) {
      return null;
    }
  },
  read(abs) {
    try {
      return new Uint8Array(fs.readFileSync(abs));
    } catch (e) {
      return null;
    }
  },
  write(abs, bytes, append) {
    try {
      if (append) {
        fs.appendFileSync(abs, bytes);
      } else {
        fs.writeFileSync(abs, bytes);
      }
      return true;
    } catch (e) {
      return false;
    }
  },
  mkdir(abs) {
    try {
      fs.mkdirSync(abs);
      return true;
    } catch (e) {
      return false;
    }
  },
  remove(abs) {
    try {
      if (fs.statSync(abs).isDirectory()) {
        fs.rmdirSync(abs);
      } else {
        fs.unlinkSync(abs);
      }
      return true;
    } catch (e) {
      return false;
    }
  },
  rename(abs, toAbs) {
    try {
      if (fs.existsSync(toAbs)) {
        return false;
      }
      fs.renameSync(abs, toAbs);
      return true;
    } catch (e) {
      return false;
    }
  }
};

// CN1_PRELOAD_JS: classes translated incrementally against this (open-world)
// bundle, evaluated before main runs -- how the incremental translation tests
// stand in for the Playground loading compiled user code.
if (process.env.CN1_PRELOAD_JS) {
  for (const file of process.env.CN1_PRELOAD_JS.split(path.delimiter)) {
    if (file) {
      global.__cn1LoadClasses(fs.readFileSync(file, 'utf8'));
    }
  }
}

const main = global.__cn1ExportedMain;
if (typeof main !== 'function') {
  console.error('no main method in ' + bundle);
  process.exit(2);
}
const args = jvm.newArray(programArgs.length, 'java_lang_String', 1);
for (let i = 0; i < programArgs.length; i++) {
  args[i] = jvm.createStringLiteral(programArgs[i]);
}
const mainThread = jvm.newObject('java_lang_Thread');
mainThread.cn1_java_lang_Thread_alive = 1;
mainThread.cn1_java_lang_Thread_name = jvm.createStringLiteral('main');
jvm.spawn(mainThread, main(args));
while (exitStatus === null && (jvm.runnable.length || timers.length)) {
  if (jvm.runnable.length) {
    jvm.drain();
    continue;
  }
  timers.sort(function(a, b) { return a.due - b.due || a.id - b.id; });
  const timer = timers.shift();
  if (!timer.cleared) {
    virtualNow = Math.max(virtualNow, timer.due);
    timer.fn();
  }
}
// exitCode rather than process.exit(): writes to a pipe are asynchronous in
// Node, and exiting outright truncates whatever the program printed last.
process.exitCode = exitStatus === null ? 0 : exitStatus;
