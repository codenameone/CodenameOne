package com.codename1.impl.android;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class FridaDetectionUtil {

    private static final String TAG = "Codename One";

    // List of common Frida process names
    private static final String[] FRIDA_PROCESSES = {
            "frida-server",
            "frida-agent",
            "frida-injector",
            "frida"
    };

    // List of common Frida libraries
    private static final String[] FRIDA_LIBRARIES = {
            "libfrida-gadget.so"
    };

    // Check for known Frida processes
    public static boolean isFridaProcessRunning() {
        try {
            Process process = Runtime.getRuntime().exec("ps");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                for (String processName : FRIDA_PROCESSES) {
                    if (line.contains(processName)) {
                        Log.e(TAG, "Frida process detected: " + processName);
                        return true;
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            Log.e(TAG, "Error checking for Frida processes: " + e.getMessage());
        }
        return false;
    }

    // Check for Frida libraries loaded in the app
    public static boolean isFridaLibraryLoaded() {
        BufferedReader reader = null;
        try {
            FileInputStream fis = new FileInputStream(new File("/proc/self/maps"));
            reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                for (String lib : FRIDA_LIBRARIES) {
                    if (line.contains(lib)) {
                        Log.e(TAG, "Frida library loaded: " + lib);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking for Frida libraries: " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing reader: " + e.getMessage());
                }
            }
        }
        return false;
    }

    // Check for suspicious system properties
    public static boolean isFridaPropertySet() {
        try {
            List<String> suspiciousProperties = new ArrayList<String>();
            suspiciousProperties.add(getSystemProperty("ro.debuggable"));

            for (String property : suspiciousProperties) {
                if ("1".equals(property)) {
                    Log.e(TAG, "Suspicious system property detected: " + property);
                    return true;
                }
            }
            if ("0".equals(getSystemProperty("ro.secure"))) {
                Log.e(TAG, "Suspicious system property detected: ro.secure=0");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking for Frida properties: " + e.getMessage());
        }
        return false;
    }

    // Get the value of a system property
    private static String getSystemProperty(String propName) {
        try {
            Class clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getMethod("get", new Class[] { String.class });
            return (String) method.invoke(null, new Object[] { propName });
        } catch (Exception e) {
            Log.e(TAG, "Error getting system property: " + e.getMessage());
            return null;
        }
    }

    // Check for Frida-specific classes
    public static boolean isFridaClassDetected() {
        try {
            Class clazz = Class.forName("re.frida.ServerManager");
            if (clazz != null) {
                Log.e(TAG, "Frida class detected: re.frida.ServerManager");
                return true;
            }
        } catch (ClassNotFoundException e) {
            // Expected if Frida is not present
        }
        return false;
    }

    // Comprehensive method to run all checks
    public static boolean isFridaDetected() {
        return isFridaProcessRunning() || isFridaLibraryLoaded() || isFridaPropertySet() || isFridaClassDetected();
    }

    // Run the detection checks and log the result
    public static void runFridaDetection(Context context) {
        if (isFridaDetected()) {
            Log.e(TAG, "Frida detected! Exiting app.");
            System.exit(0);
        } else {
            Log.i(TAG, "No Frida detection evidence found.");
        }
    }
}
