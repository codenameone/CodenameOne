self.window = self;
self.global = self;
// ``getParameterByName`` is defined in fontmetrics.js (main thread) and used
// by JSBodies that read URL query params (cn1SafariBacksideHookDelay,
// pixelRatio, isDesktop, isTablet, baseFont, density, ...). Those JSBodies
// run inside the worker, where ``window`` aliases ``self`` — fontmetrics.js
// never loads here, so the lookup is undefined and the JSBody throws.
//
// On Safari/WebKit the throw propagates out of ``HTML5Implementation_*.handleEvent``
// and aborts the click handler before the action listener fires; the
// Initializr's "Hello World" dialog and Toolbar side menu never appear. On
// Chromium the Safari-specific ``_safariBacksideHookDelay`` path is gated by
// ``isSafari()`` and the throw doesn't fire — so the bug is Safari-only.
//
// We define a worker-local equivalent that reads from ``__cn1LocationSearch``
// (forwarded by the main thread on START so the worker can see query params).
self.getParameterByName = function(name) {
  var search = self.__cn1LocationSearch || '';
  var regex = new RegExp("[\\?&]" + name + "=([^&#]*)");
  var results = regex.exec(search);
  if (results == null) {
    return '';
  }
  return decodeURIComponent(results[1].replace(/\+/g, ' '));
};
/*__IMPORTS__*/
if (typeof self.__parparInstallNativeBindings === 'function') {
  self.__parparInstallNativeBindings();
}
self.onmessage = function(event) {
  if (!event || !event.data) {
    return;
  }
  const protocol = jvm.protocol.messages;
  if (event.data.type === protocol.START) {
    self.__cn1LocationSearch = event.data.locationSearch ? String(event.data.locationSearch) : '';
    // Main thread forwarded window.devicePixelRatio — surface it on self so
    // existing `win.devicePixelRatio` lookups (via the `self.window = self;`
    // alias above) return the real ratio instead of undefined.
    if (typeof event.data.devicePixelRatio === 'number' && event.data.devicePixelRatio > 0) {
      self.devicePixelRatio = event.data.devicePixelRatio;
    }
    jvm.start();
  } else if (event.data.type === protocol.PROTOCOL_INFO
          || event.data.type === protocol.UI_EVENT
          || event.data.type === protocol.EVENT
          || event.data.type === protocol.HOST_CALLBACK
          || event.data.type === protocol.TIMER_WAKE
          || event.data.type === 'worker-callback') {
    // worker-callback: DOM event fired on the main thread, now forwarded
    // back to a worker-side JS function registered via the function->
    // callback-id token dance in toHostTransferArg / browser_bridge.js
    // mapHostArgs. See jvm.workerCallbacks for the registry shape.
    jvm.handleMessage(event.data);
  }
};
