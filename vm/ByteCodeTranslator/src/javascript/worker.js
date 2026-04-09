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
    jvm.start();
  } else if (event.data.type === protocol.PROTOCOL_INFO
          || event.data.type === protocol.UI_EVENT
          || event.data.type === protocol.EVENT
          || event.data.type === protocol.HOST_CALLBACK
          || event.data.type === protocol.TIMER_WAKE) {
    jvm.handleMessage(event.data);
  }
};
