(function(global) {
  function log(line) {
    if (global.console && typeof global.console.log === 'function') {
      global.console.log('PARPAR:' + line);
    }
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
    }
  }

  function installWorkerMode() {
    log('worker-mode');
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

  function loadScript(src, callback) {
    log('load-script:' + src);
    var script = document.createElement('script');
    script.src = src;
    script.onload = function() {
      callback(null);
    };
    script.onerror = function(err) {
      callback(err || new Error('Failed to load ' + src));
    };
    document.head.appendChild(script);
  }

  function installMainThreadMode(onReady) {
    log('main-thread-mode');
    global.__cn1ParparDispatchMessage = function(data) {
      handleVmMessage(data, null);
    };
    loadScript('parparvm_runtime.js', function(runtimeErr) {
      if (runtimeErr) {
        global.__parparError = { type: 'error', message: String(runtimeErr) };
        return;
      }
      loadScript('translated_app.js', function(appErr) {
        if (appErr) {
          global.__parparError = { type: 'error', message: String(appErr) };
          return;
        }
        log('translated-app-ready');
        onReady();
      });
    });
  }

  var appStarter = null;

  global.startParparVmApp = function() {
    log('startParparVmApp');
    global.cn1Initialized = true;
    if (appStarter) {
      log('appStarter-present');
      appStarter();
    }
  };

  if (global.cn1UseWorkerVm) {
    var worker = installWorkerMode();
    appStarter = function() {
      worker.postMessage({ type: 'start' });
    };
  } else {
    installMainThreadMode(function() {
      appStarter = function() {
        if (global.jvm && typeof global.jvm.start === 'function') {
          log('jvm.start.begin');
          global.jvm.start();
          log('jvm.start.end');
        }
      };
      log('appStarter-ready');
      if (global.cn1Initialized) {
        appStarter();
      }
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', global.startParparVmApp);
  } else {
    global.startParparVmApp();
  }
})(self);
