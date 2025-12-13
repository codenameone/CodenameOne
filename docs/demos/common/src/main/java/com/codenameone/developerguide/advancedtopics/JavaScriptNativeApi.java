package com.codenameone.developerguide.advancedtopics;

// tag::jsPrintMethod[]
public interface JavaScriptNativeApi {
    void print(String str);
}
// end::jsPrintMethod[]

// tag::jsAddTwoArgs[]
interface JavaScriptNativeApiAddTwoArgs {
    int add(int a, int b);
}
// end::jsAddTwoArgs[]

// tag::jsAddArray[]
interface JavaScriptNativeApiAddArray {
    int add(int[] a);
}
// end::jsAddArray[]
