self.window = self;
self.global = self;
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
