/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.videojs;

import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSProperty;

/**
 *
 * @author shannah
 */
public class MediaTool {
    public static interface MediaResult extends JSObject{
        @JSProperty
        public int getWidth();
        
        @JSProperty
        public int getHeight();
        
        
    }
    
    @JSFunctor
    private static interface MediaQueryCallback extends JSObject {
        public void onResult(MediaResult res);
    }
    
    @JSBody(params={"preferredWidth", "preferredHeight", "callback"}, 
            script="navigator.mediaDevices.getUserMedia({\n" +
                "    video:{\n" +
                "        width:preferredWidth, height:preferredHeight\n" +
                "    }\n" +
                "}).then(function(stream){\n" +
                "    console.log('We have our stream');\n" +
                "    var w = stream.getVideoTracks()[0].getSettings().width;\n" +
                "    var h = stream.getVideoTracks()[0].getSettings().height;\n" +
                "    stream.getTracks().forEach(function(track) {\n" +
                "        track.stop();\n" +
                "    });\n" +
                "    callback({\n" +
                "        width: w, \n" +
                "        height: h\n" +
                "    });\n" +
                "}).catch(function(error){\n" +
                "    console.log('Failed to get user media');\n" +
                "    console.error(error);\n" + 
                "    callback({\n" +
                "        width:0, height:0, error:error.message\n" +
                "    });\n" +
                "});")
    private static native void query_(int preferredWidth, int preferredHeight, MediaQueryCallback callback);
    
    public MediaResult query(int preferredWidth, int preferredHeight) {
        final MediaResult[] result = new MediaResult[1];
        Log.p("Querying capture devices for "+preferredWidth+"x"+preferredHeight);
        query_(preferredWidth, preferredHeight, new MediaQueryCallback() {
            @Override
            public void onResult(final MediaResult res) {
                new Thread(new Runnable() {
                    public void run() {
                        synchronized(result) {
                            result[0] = res;
                            result.notify();
                        }
                    }
                }).start();
            }
        });
        
        while (result[0] == null) {
            synchronized(result) {
                Util.wait(result);
            }
        }
        
        return result[0];
    }

}
