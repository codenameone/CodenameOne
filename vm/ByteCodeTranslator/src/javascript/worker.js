self.window = self;
self.global = self;
/*__IMPORTS__*/
self.onmessage = function(event) {
  if (!event || !event.data) {
    return;
  }
  if (event.data.type === 'start') {
    jvm.start();
  } else if (event.data.type === 'ui-event') {
    jvm.handleMessage(event.data);
  }
};
