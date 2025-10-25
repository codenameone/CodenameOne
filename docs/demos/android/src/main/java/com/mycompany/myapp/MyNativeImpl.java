package com.mycompany.myapp;

import android.util.Log;

// tag::myNativeAndroidImpl[]
public class MyNativeImpl {
    public String helloWorld(String param) {
        Log.d("MyApp", param);
        return "Tada";
    }

    public boolean isSupported() {
        return true;
    }
}
// end::myNativeAndroidImpl[]
