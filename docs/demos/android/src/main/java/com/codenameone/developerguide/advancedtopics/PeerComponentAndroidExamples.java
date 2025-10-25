package com.codenameone.developerguide.advancedtopics;

import android.view.View;

// tag::androidPeerImplementation[]
class AndroidPeerImplementation {
    public View createPeer() {
        return null;
    }
}
// end::androidPeerImplementation[]

// tag::androidAllTypesSignature[]
class AndroidAllTypesImplementation {
    public void test(byte param, boolean param1, char param2,
                     short param3, int param4, long param5, float param6,
                     double param7, String param8, byte[] param9,
                     boolean[] param10, char[] param11, short[] param12,
                     int[] param13, long[] param14, float[] param15,
                     double[] param16, android.view.View param17) {
    }
}
// end::androidAllTypesSignature[]
