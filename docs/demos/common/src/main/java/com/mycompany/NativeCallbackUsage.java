package com.mycompany;

// tag::nativeCallbackUsage[]
public class NativeCallbackUsage {
    public void invokeCallbacks() {
        NativeCallback.callback();
        NativeCallback.callback("My Arg");
    }
}
// end::nativeCallbackUsage[]
