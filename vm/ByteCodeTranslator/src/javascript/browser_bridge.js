(function(global) {
  function postHostCallback(worker, id, value, errorMessage) {
    if (errorMessage == null) {
      worker.postMessage({ type: 'host-callback', id: id, value: value });
    } else {
      worker.postMessage({ type: 'host-callback', id: id, error: true, errorMessage: String(errorMessage) });
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
    invoke: function(symbol, args, worker, id) {
      var handler = this.handlers[symbol];
      if (!handler) {
        postHostCallback(worker, id, null, 'Unhandled host call ' + symbol);
        return;
      }
      try {
        normalizeHostResult(handler.apply(null, args || []), function(value, err) {
          postHostCallback(worker, id, value, err);
        });
      } catch (err) {
        postHostCallback(worker, id, null, err);
      }
    }
  };

  var worker = new Worker('worker.js');
  global.__parparWorker = worker;
  global.__parparMessages = [];
  global.cn1Initialized = false;
  global.cn1Started = false;

  worker.onmessage = function(event) {
    var data = event.data;
    global.__parparMessages.push(data);
    if (!data) {
      return;
    }
    if (data.type === 'host-call') {
      hostBridge.invoke(data.symbol, data.args || [], worker, data.id);
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
  };

  worker.onerror = function(error) {
    global.__parparError = {
      type: 'error',
      message: error && error.message ? error.message : String(error)
    };
  };

  global.startParparVmApp = function() {
    global.cn1Initialized = true;
    worker.postMessage({ type: 'start' });
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', global.startParparVmApp);
  } else {
    global.startParparVmApp();
  }
})(self);
