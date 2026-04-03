/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5.videojs;

import com.codename1.components.ToastBar;
import com.codename1.impl.html5.HTML5Implementation;
import com.codename1.impl.html5.tools.CSSTool;
import com.codename1.impl.html5.tools.ScriptTool;
import com.codename1.teavm.jso.io.Blob;
import com.codename1.ui.CN;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSProperty;
import com.codename1.html5.js.browser.TimerHandler;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.dom.Event;
import com.codename1.html5.js.dom.EventListener;
import com.codename1.html5.js.dom.HTMLVideoElement;

/**
 *
 * @author shannah
 */
public class VideoJS {
    private static final String VIDEO_ID="cn1-video-capture";
    private List<VideoListener> listeners = new ArrayList<VideoListener>();
    private static final String VIDEOJS_BASE="js/videojs/";
    private HTMLVideoElement videoElement;
    private Player player;
    
    public Player getPlayer() {
        return player;
    }
    
    public HTMLVideoElement getVideoElement() {
        return videoElement;
    }
    
    public final void setVideoElement(HTMLVideoElement el) {
        videoElement = el;
    }
    
    public VideoJS(HTMLVideoElement el, Options opts) throws IOException {
        setVideoElement(el);
        init(opts);
    }
    private int fitVideoTimeoutHandle;
    private void init(Options opts) throws IOException {
        if (HTML5Implementation.isIOS()) {
            setDeviceButtonEnabled(opts, true);
        }
        ToastBar.Status loadingStatus = ToastBar.getInstance().createStatus();
        loadingStatus.setMessage("Loading Video Recording libraries from server.  Please wait...");
        loadingStatus.showDelayed(100);
        try {
            CSSTool.getInstance().load(VIDEOJS_BASE+"video-js.min.css");
            CSSTool.getInstance().load(VIDEOJS_BASE+"videojs.record.min.css");

            ScriptTool.getInstance().requireOrdered( 
                    VIDEOJS_BASE+"video.min.js", 
                    VIDEOJS_BASE+"RecordRTC.min.js", 
                    VIDEOJS_BASE+"adapter.js",
                    VIDEOJS_BASE+"videojs.record.min.js",
                    VIDEOJS_BASE+"browser-workarounds.js"
                    );
        } finally {
            loadingStatus.clear();
        }
        if (videoElement == null) {
            HTMLVideoElement video = (HTMLVideoElement)Window.current().getDocument().createElement("video");
            videoElement = video;
            video.setAttribute("id", VIDEO_ID);
            Window.current().getDocument().getBody().appendChild(video);
        }
        if (HTML5Implementation.isIOS()) {
            videoElement.setAttribute("muted", "");
            videoElement.setAttribute("playsinline", "");
        }
        fitVideoTimeoutHandle = Window.setInterval(new TimerHandler() {
            @Override
            public void onTimer() {
                if (videoElement == null || videoElement.getParentNode() == null) {
                    Window.clearInterval(fitVideoTimeoutHandle);
                    return;
                }
                fitVideo(videoElement);
            }
            
        }, 500);
        player = newPlayer(VIDEO_ID, opts);
        on(player, "deviceError", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                fireEventFromJSThread(EVENT_TYPE_DEVICE_ERROR, player.getDeviceErrorCode());
            }
        });
        on(player, "error", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                fireEventFromJSThread(EVENT_TYPE_DEVICE_ERROR, "An error occurred");
            }
        });
        on(player, "startRecord", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                fireEventFromJSThread(EVENT_TYPE_START_RECORD, null);
            }
        });
        on(player, "finishRecord", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                fireEventFromJSThread(EVENT_TYPE_FINISH_RECORD, player.getRecordedData());
            }
        });
        if (!HTML5Implementation.isIOS()) {
            getDevice_(player);
        }
        
    }
    
    private static final int EVENT_TYPE_DEVICE_ERROR=0;
    private static final int EVENT_TYPE_ERROR=1;
    private static final int EVENT_TYPE_START_RECORD=2;
    private static final int EVENT_TYPE_FINISH_RECORD=3;
    
    private void fireEventFromJSThread(final int type, final Object param) {
        
        new Thread(new Runnable() {
            public void run() {
                fireEvent(type, param);
            }
        }).start();
            
        

    }
    
    private void fireEvent(final int type, final Object param) {
        if (listeners.isEmpty()) {
            return;
        }
        
        if (!CN.isEdt()) {
            CN.callSerially(new Runnable() {
                public void run() {
                    fireEvent(type, param);
                }
            });
            return;
        }
        ArrayList<VideoListener> queue = new ArrayList<VideoListener>(listeners);
        for (VideoListener l : queue) {
            switch (type) {
                case EVENT_TYPE_DEVICE_ERROR:
                    l.onDeviceError((String)param);
                    break;
                case EVENT_TYPE_ERROR:
                    l.onError((String)param);
                    break;
                case EVENT_TYPE_START_RECORD:
                    l.onStartRecord();
                    break;
                case EVENT_TYPE_FINISH_RECORD:
                    l.onFinishRecord((Blob)param);
                    break;
            }
        }
    }

    public void destroy() {
        destroy_(player);
    }
    
    public static interface VideoListener {
        
        public void onDeviceError(String errorCode);
        public void onError(String message);
        public void onStartRecord();
        public void onFinishRecord(Blob recordedData);
    }
    
    public void addListener(VideoListener l) {
        listeners.add(l);
    }
    
    public void removeListener(VideoListener l) {
        listeners.remove(l);
    }
    
    public static interface Player extends JSObject {
        public boolean isRecording();
        public String getRecordType();
        public void saveAs();
        
        public void reset();
        public void stopDevice();
        public int getDuration();
        public int getCurrentTime();
        public void start();
        public void stop();
        public void pause();
        public void resume();
        
        @JSProperty
        public String getDeviceErrorCode();
        
        @JSProperty
        public Blob getRecordedData();
        
        
    }
    
    @JSBody(params={"videoId", "opts"}, script="console.log(opts); return videojs(videoId, opts)")
    private native static Player newPlayer(String videoId, Options opts);
    
    @JSBody(params={"player", "eventName", "l"}, script="player.on(eventName, l)")
    private native static void on(Player player, String eventName, EventListener l);
    
    public static interface Options extends JSObject {
        @JSProperty
        public boolean isControls();
        
        @JSProperty
        public void setControls(boolean controls);
        
        @JSProperty
        public int getWidth();
        
        @JSProperty
        public void setWidth(int width);
        
        @JSProperty
        public void getHeight();
        
        @JSProperty
        public void setHeight(int height);
        
        @JSProperty
        public boolean isFluid();
        
        @JSProperty
        public void setFluid(boolean fluid);
        
        @JSProperty
        public void setPlugins(Plugins plugins);
        
        @JSProperty
        public Plugins getPlugins();
        
    }
    
    public static interface Plugins extends JSObject {
        @JSProperty
        public RecordOptions getRecord();
        
        @JSProperty
        public void setRecord(RecordOptions opts);
    }
    
    public static interface RecordOptions extends JSObject {
        @JSProperty
        public void setAudio(boolean audio);
        
        @JSProperty
        public boolean isAudio();
        
        @JSProperty
        public void setMaxLength(int len);
        
        @JSProperty
        public int getMaxLength();
        
       
        @JSProperty
        public void setDebug(boolean debug);
        
        @JSProperty
        public boolean isDebug();
        
        @JSProperty
        public void setFrameWidth(int width);
        
        @JSProperty
        public void setFrameHeight(int height);
        
        @JSProperty
        public int getFrameWidth();
        
        @JSProperty
        public int getFrameHeight();
        
        @JSProperty
        public MediaStreamConstraints getVideo();
        
        @JSProperty
        public void setVideo(MediaStreamConstraints video);
    }
    
    public static interface MediaStreamConstraints extends JSObject {
        @JSProperty
        public int getWidth();
        
        @JSProperty
        public void setWidth(int w);
        
        @JSProperty
        public void setHeight(int h);
        
        @JSProperty
        public int getHeight();
    }
    
    @JSBody(params={}, script="return {controls: true, width: 320, height: 240, fluid:false, plugins: {}, controlBar: {\n" +
"    fullscreenToggle: false,deviceButton:false,pipToggle:false, \n" +
"    volumePanel: false\n" +
"}}")
    public static native Options newOptions();
    @JSBody(params={"opts","deviceButton"}, script="opts.controlBar.deviceButton = deviceButton")
    private static native void setDeviceButtonEnabled(Options opts, boolean deviceButton);
    
    @JSBody(params={}, script="return {audio:true, video:true, maxLength:10, debug:true}")
    public static native RecordOptions newRecordOptions();
    
    @JSBody(params={}, script="return {}")
    public static native MediaStreamConstraints newMediaStreamConstraints();
    
    @JSBody(params={"recordOpts"}, script="return {record:recordOpts}")
    public static native Plugins newPlugins(RecordOptions recordOpts);
    
    @JSBody(params={"player"}, script="player.record().destroy()")
    private static native void destroy_(Player player);
    
    @JSBody(params={"player"}, script="player.record().getDevice()")
    private static native void getDevice_(Player player);
    
    @JSBody(params={"video"}, script="var videoRatio = (video.height||50) / (video.width||50);\n" +
        "var windowRatio = window.innerHeight / window.innerWidth;\n" +
        "\n" +
        "    if (windowRatio < videoRatio) {\n" +
        "        if (window.innerHeight > 50) { \n" +
        "                video.height = window.innerHeight;\n" +
        "        } else {\n" +
        "            video.height = 50;\n" +
        "    }\n" +
        "    } else {\n" +
        "        video.width = window.innerWidth;\n" +
        "    }")
    private static native void fitVideo(HTMLVideoElement video);
}

