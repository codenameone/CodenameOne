package com.mycompany;

// tag::nativeCallback[]
public class NativeCallback {
    public static void callback() {
        // do stuff
    }

    // tag::nativeCallbackInt[]
    public static void callback(int arg) {
        // do stuff
    }
    // end::nativeCallbackInt[]

    // tag::nativeCallbackString[]
    public static void callback(String arg) {
        // do stuff
    }
    // end::nativeCallbackString[]

    // tag::nativeCallbackReturn[]
    public static int callback(int arg) {
        // do stuff
        return arg;
    }
    // end::nativeCallbackReturn[]
}
// end::nativeCallback[]
