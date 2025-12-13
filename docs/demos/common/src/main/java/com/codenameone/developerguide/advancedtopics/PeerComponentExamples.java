package com.codenameone.developerguide.advancedtopics;

import com.codename1.ui.PeerComponent;

// tag::peerComponentApi[]
interface NativePeerProvider {
    PeerComponent createPeer();
}
// end::peerComponentApi[]

// tag::allTypesSignature[]
interface AllTypesNativeApi {
    void test(byte b, boolean boo, char c, short s,
              int i, long l, float f, double d, String ss,
              byte[] ba, boolean[] booa, char[] ca, short[] sa, int[] ia,
              long[] la, float[] fa, double[] da,
              PeerComponent cmp);
}
// end::allTypesSignature[]

// tag::javascriptAllTypesSignature[]
class JavaScriptAllTypesImplementation {
    public void test(byte param, boolean param1, char param2, short param3, int param4,
                     long param5, float param6, double param7, String param8, byte[] param9,
                     boolean[] param10, char[] param11, short[] param12, int[] param13,
                     long[] param14, float[] param15, double[] param16,
                     PeerComponent param17) {
    }
}
// end::javascriptAllTypesSignature[]
