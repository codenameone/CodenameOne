package com.mycompany.myapp;

import com.codename1.system.NativeInterface;

// tag::myNativeInterface[]
public interface MyNative extends NativeInterface {
    String helloWorld(String hi);
}
// end::myNativeInterface[]
