package com.mycompany.myapp;

import android.util.Log;

// tag::myNativeAndroidImpl[]
public class MyNativeImpl { // <2> <3>
    public String helloWorld(String param) {
        Log.d("MyApp", param); // <1>
        return "Tada";
    }

    public boolean isSupported() {
        return true; // <4>
    }
}
// end::myNativeAndroidImpl[]
