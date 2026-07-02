// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::the-edt-event-dispatch-thread-java-001[]
while(codenameOneRunning) {
     performEventCallbacks();
     performCallSeriallyCalls();
     drawGraphicsAndAnimations();
     sleepUntilNextEDTCycle();
}
// end::the-edt-event-dispatch-thread-java-001[]

// tag::the-edt-event-dispatch-thread-java-002[]
// this code is executing in a separate thread
final String res = methodThatTakesALongTime();
Display.getInstance().callSerially(new Runnable() {
     public void run() {
          // this occurs on the EDT so I can make changes to UI components
          resultLabel.setText(res);
     }
});
// end::the-edt-event-dispatch-thread-java-002[]

// tag::the-edt-event-dispatch-thread-java-003[]
// this code is executing in a separate thread
String res = methodThatTakesALongTime();
Display.getInstance().callSerially(() -> resultLabel.setText(res));
// end::the-edt-event-dispatch-thread-java-003[]

// tag::the-edt-event-dispatch-thread-java-004[]
// this code is executing in a separate thread
methodThatTakesALongTime();
Display.getInstance().callSeriallyAndWait(() -> {
  // this occurs on the EDT so I can make changes to UI components
  globalFlag = Dialog.show("Are You Sure?", "Do you want to continue?", "Continue", "Stop");
});
// this code is executing the separate thread
// global flag was already set by the call above
if(!globalFlag) {
   return;
}
otherMethod();
// end::the-edt-event-dispatch-thread-java-004[]

// tag::the-edt-event-dispatch-thread-java-005[]
doOperationA();
doOperationB();
doOperationC();
// end::the-edt-event-dispatch-thread-java-005[]

// tag::the-edt-event-dispatch-thread-java-006[]
doOperationA();
new Thread() {
    public void run() {
         doOperationB();
    }
}.start();
doOperationC();
// end::the-edt-event-dispatch-thread-java-006[]

// tag::the-edt-event-dispatch-thread-java-007[]
updateUIToLoadingStatus();
readAndParseFile();
updateUIWithContentOfFile();
// end::the-edt-event-dispatch-thread-java-007[]

// tag::the-edt-event-dispatch-thread-java-008[]
updateUIToLoadingStatus();
new Thread() {
    public void run() {
          readAndParseFile();
          updateUIWithContentOfFile();
    }
}.start();
// end::the-edt-event-dispatch-thread-java-008[]

// tag::the-edt-event-dispatch-thread-java-009[]
updateUIToLoadingStatus();
new Thread() {
    public void run() {
          readAndParseFile();
          Display.getInstance().callSerially(new Runnable() {
               public void run() {
                     updateUIWithContentOfFile();
               }
          });
    }
}.start();
// end::the-edt-event-dispatch-thread-java-009[]

// tag::the-edt-event-dispatch-thread-java-010[]
updateUIToLoadingStatus();
Display.getInstance().invokeAndBlock(new Runnable() {
    public void run() {
          readAndParseFile();
    }
});
updateUIWithContentOfFile();
// end::the-edt-event-dispatch-thread-java-010[]

// tag::the-edt-event-dispatch-thread-java-011[]
updateUIToLoadingStatus();
Display.getInstance().invokeAndBlock(() -> readAndParseFile());
updateUIWithContentOfFile();
// end::the-edt-event-dispatch-thread-java-011[]

// tag::the-edt-event-dispatch-thread-java-012[]
public void actionPerformed(ActionEvent ev) {
  // will return true if the user clicks "OK"
  if(!Dialog.show("Question", "How Are You", "OK", "Not OK")) {
  // ask what went wrong...
  }
}
// end::the-edt-event-dispatch-thread-java-012[]

// tag::the-edt-event-dispatch-thread-java-013[]
while(codenameOneRunning) {
     performEventCallbacks();
     performCallSeriallyCalls();
     drawGraphicsAndAnimations();
     sleepUntilNextEDTCycle();
}
// end::the-edt-event-dispatch-thread-java-013[]

// tag::the-edt-event-dispatch-thread-java-014[]
void invokeAndBlock(Runnable r) {
    openThreadForR(r);
    while(r is still running) {
         performEventCallbacks();
         performCallSeriallyCalls();
         drawGraphicsAndAnimations();
         sleepUntilNextEDTCycle();
    }
}
// end::the-edt-event-dispatch-thread-java-014[]
