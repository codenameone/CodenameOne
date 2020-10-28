/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.impl.javase.cef;

import com.codename1.impl.javase.AbstractBrowserWindowSE;
import com.codename1.impl.javase.BrowserWindowFactory;
import com.codename1.impl.javase.fx.FXBrowserWindowSE;
import com.codename1.impl.javase.JavaSEPort;
import static com.codename1.impl.javase.JavaSEPort.checkForPermission;
import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.media.AbstractMedia;
import com.codename1.media.AsyncMedia;
import com.codename1.media.Media;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.events.ActionEvent;
import com.codename1.util.AsyncResource;
import java.awt.EventQueue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;

/**
 *
 * @author shannah
 */
public class JavaCEFSEPort extends JavaSEPort {
    
    private static boolean cefExists;
    
    @Override
    public void init(Object m) {
        super.init(m);
        try {
            Class.forName("org.cef.CefApp");
            
            cefExists = true;
        } catch (Throwable ex) {
        }
    }
    
    
    
    
    public boolean isNativeBrowserComponentSupported() {
        return cefExists && !blockNativeBrowser;
        //return false;
    }
    
    
    public PeerComponent createBrowserComponent(final Object parent) {
        boolean useWKWebView = "true".equals(Display.getInstance().getProperty("BrowserComponent.useWKWebView", "false"));
        if (useWKWebView) {
            if (!useWKWebViewChecked) {
                useWKWebViewChecked = true;
                Map<String, String> m = Display.getInstance().getProjectBuildHints();
                if(m != null) {
                    if(!m.containsKey("ios.useWKWebView")) {
                        Display.getInstance().setProjectBuildHint("ios.useWKWebView", "true");
                    }
                }
            }
        }
        return createCEFBrowserComponent(parent);
        
    }
    
    protected BrowserWindowFactory createBrowserWindowFactory() {
        return new BrowserWindowFactory() {
            @Override
            public AbstractBrowserWindowSE createBrowserWindow(String startURL) {
                return new FXBrowserWindowSE(startURL);
            }

       };
    }
    
    public PeerComponent createCEFBrowserComponent(final Object parent) {
        final PeerComponent[] out = new PeerComponent[1];
        if (!EventQueue.isDispatchThread()) {
            try {
                EventQueue.invokeAndWait(new Runnable() {
                    public void run() {
                        
                            out[0] = createCEFBrowserComponent(parent);
                        }
                });
            } catch (Throwable ex) {
                throw new RuntimeException("Failed to create CEF browser", ex);
            }
            
            return out[0];
        } else {
            
            return CEFBrowserComponent.create((BrowserComponent)parent);
        }
    }

   
    
    class CodenameOneMediaPlayer extends AbstractMedia {
        //private java.util.Timer endMediaPoller;
        private Runnable onCompletion;
        private List<Runnable> completionHandlers;
        //private javafx.scene.media.MediaPlayer player;
        private CEFBrowserComponent bc;
//        private MediaPlayer player;
        private boolean realized = false;
        private boolean isVideo;
        //private javafx.embed.swing.JFXPanel videoPanel;
        private JFrame frm;
        private boolean playing = false;
        private boolean nativePlayerMode;
        private AsyncResource<Media> _callback;
        
        private int currTime;
        private int duration;
        private int volume;
        private String lastErrorMessage;
        
        
        /**
         * This is a callback for the JavaFX media player that is supposed to fire
         * when the media is paused.  Unfortunately this is unreliable as the status
         * events seem to stop working after the first time it is paused.  
         * We use a poller (the endMediaPoller timer) to track the status of the video
         * so that the change listeners are fired.  This really sucks!
         */
        private Runnable onPaused = new Runnable() {
            public void run() {
                
                playing = false;
                
                fireMediaStateChange(AsyncMedia.State.Paused);
            }
        };
        /**
         * This is a callback for the JavaFX media player that is supposed to fire
         * when the media is paused.  Unfortunately this is unreliable as the status
         * events seem to stop working after the first time it is paused.  
         * We use a poller (the endMediaPoller timer) to track the status of the video
         * so that the change listeners are fired.  This really sucks!
         */
        private Runnable onPlaying = new Runnable() {
            @Override
            public void run() {
                playing = true;
                
                fireMediaStateChange(AsyncMedia.State.Playing);
            }
            
        };
        
        /**
         * This is a callback for the JavaFX media player that is supposed to fire
         * when the media is paused.  Unfortunately this is unreliable as the status
         * events seem to stop working after the first time it is paused.  
         * We use a poller (the endMediaPoller timer) to track the status of the video
         * so that the change listeners are fired.  This really sucks!
         */
        private Runnable onError = new Runnable() {
            public void run() {
                if (_callback != null && !_callback.isDone()) {
                    _callback.error(new IOException(lastErrorMessage));
                    return;
                } else {
                    Log.p(lastErrorMessage);
                }
                fireMediaError(createMediaException(lastErrorMessage, MediaErrorType.Unknown));
                if (!playing) {
                    fireMediaStateChange(AsyncMedia.State.Playing);
                    fireMediaStateChange(AsyncMedia.State.Paused);
                }

            }
        };

        
        
        private boolean isPlayable(String uri) {
            return true;
        }
        
        public CodenameOneMediaPlayer(String uri, boolean isVideo, JFrame f,  final Runnable onCompletion, final AsyncResource<Media> callback) throws IOException {
            init(uri, isVideo, f, onCompletion, callback);
        }
        public void init(String uri, boolean isVideo, JFrame f,  final Runnable onCompletion, final AsyncResource<Media> callback) throws IOException {
            
            _callback = callback;
            final JSONParser parser = new JSONParser();
            String mediaTag = isVideo ? "video" : "audio";
            String style = "<style type='text/css'>document, body {padding:0;margin:0; width:100%; height: 100%} video, audio {margin:0; padding:0; width:100%; height: 100%}</style>";
            String script = "<script>window.cn1Media = document.getElementById('cn1Media');"
                    + "function callback(data){ cefQuery({request:'shouldNavigate:'+JSON.stringify(data), onSuccess: function(response){}, onFailure:function(error_code, error_message) { console.log(error_message)}});}"
                    + "cn1Media.addEventListener('pause', function(){ callback({'state':'paused'})});"
                    + "cn1Media.addEventListener('play', function(){ callback({'state':'playing'})});"
                    + "cn1Media.addEventListener('ended', function(){ callback({'state':'ended'})});"
                    + "cn1Media.addEventListener('durationchange', function(){ callback({'duration': Math.floor(cn1Media.duration * 1000)})});"
                    + "cn1Media.addEventListener('timeupdate', function(){ callback({'time': Math.floor(cn1Media.currentTime * 1000)})});"
                    + "cn1Media.addEventListener('volumechange', function(){ callback({'volume': Math.round(cn1Media.volume * 100)})});"
                    + "cn1Media.addEventListener('error', function(){ var msg = 'Unknown Error'; try {msg = cn1Media.error.message + '. Code='+cn1Media.error.code;}catch(e){} callback({'error': msg})});</script> ";
            String html = "<!doctype html><html><head>"+style+"</head><body><"+mediaTag+" id='cn1Media' width='640' height='480' style='width:100%;height:100%' src='"+uri+"'/>"+script+"</body></html>";
            
            
            final String url = "data:text/html,"+Util.encodeUrl(html);
            bc = CEFBrowserComponent.create(url, new CEFBrowserComponentListener() {
                @Override
                public void onLoad(ActionEvent e) {
                    if (!callback.isDone()) {
                        callback.complete(CodenameOneMediaPlayer.this);
                    }
                }

                @Override
                public void onError(ActionEvent e) {
                    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    
                }

                @Override
                public void onStart(ActionEvent e) {
                    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public boolean shouldNavigate(final String url) {
                    if (url.startsWith("{")) {
                        CN.callSerially(new Runnable() {
                            public void run() {
                                try {
                                    Map m = parser.parseJSON(new StringReader(url));
                                    if (m.containsKey("time")) {
                                        currTime = ((Number)m.get("time")).intValue();
                                    }
                                    if (m.containsKey("duration")) {
                                        duration = ((Number)m.get("duration")).intValue();
                                    }
                                    if (m.containsKey("volume")) {
                                        volume = ((Number)m.get("volume")).intValue();
                                    }
                                    if (m.containsKey("state")) {
                                        String mState = (String)m.get("state");
                                        if ("playing".equals(mState)) {
                                            if (!playing) {
                                                onPlaying.run();
                                            }
                                        } else if ("paused".equals(mState)) {
                                            if (playing) {
                                                onPaused.run();
                                            }
                                        } else if ("ended".equals(mState)) {
                                            if (playing) {
                                                onPaused.run();
                                            }
                                            CodenameOneMediaPlayer.this.onCompletion.run();
                                        } else if ("canplay".equals(mState)) {
                                            
                                        }
                                    }
                                    if (m.containsKey("error")) {
                                        
                                        lastErrorMessage = String.valueOf(m.get("error"));
                                        onError.run();
                                    }
                                } catch (IOException ex) {
                                    Log.e(ex);
                                }

                            }
                            
                        });
                        return false;
                        
                    }
                    return true;
                }
                
                
                
            });
            
            
            if (onCompletion != null) {
                addCompletionHandler(onCompletion);
            }
            
            this.onCompletion = new Runnable() {

                @Override
                public void run() {
                    if (callback != null && !callback.isDone()) {
                        callback.complete(CodenameOneMediaPlayer.this);
                    }
                    playing = false;
                    
                    fireMediaStateChange(AsyncMedia.State.Paused);
                    fireCompletionHandlers();
                }
                
            };
            this.isVideo = isVideo;
            this.frm = f;
            
        }

        private com.codename1.media.AsyncMedia.MediaException createMediaException(String message, AsyncMedia.MediaErrorType type) {
            
            return new com.codename1.media.AsyncMedia.MediaException(type, message);
        }
        
      
        
        public CodenameOneMediaPlayer(InputStream stream, String mimeType, JFrame f, final Runnable onCompletion, final AsyncResource<Media> callback) throws IOException {
            String suffix = guessSuffixForMimetype(mimeType);

            File temp = File.createTempFile("mtmp", suffix);
            temp.deleteOnExit();
            FileOutputStream out = new FileOutputStream(temp);
            byte buf[] = new byte[1024];
            int len = 0;
            while ((len = stream.read(buf, 0, buf.length)) > -1) {
                out.write(buf, 0, len);
            }
            stream.close();

            
            this.isVideo = mimeType.contains("video");
            this.frm = f;
            init(temp.getAbsolutePath(), isVideo, f, onCompletion, callback);
            
        }

        
        private void fireCompletionHandlers() {
            if (completionHandlers != null && !completionHandlers.isEmpty()) {
                
                Display.getInstance().callSerially(new Runnable() {

                    @Override
                    public void run() {
                        if (completionHandlers != null && !completionHandlers.isEmpty()) {
                            List<Runnable>  toRun;

                            synchronized(CodenameOneMediaPlayer.this) {
                                toRun = new java.util.ArrayList<Runnable>(completionHandlers);
                            }
                            for (Runnable r : toRun) {
                                r.run();
                            }
                        }
                    }

                });
            }
        }
        
        public void addCompletionHandler(Runnable onCompletion) {
            synchronized(this) {
                if (completionHandlers == null) {
                    completionHandlers = new java.util.ArrayList<Runnable>();
                }

                completionHandlers.add(onCompletion);
            }
        }

        public void removeCompletionHandler(Runnable onCompletion) {
            if (completionHandlers != null) {
                synchronized(this) {
                    completionHandlers.remove(onCompletion);
                }
            }
        }
        
        public void cleanup() {
            pause();
            if (bc != null) {
                bc.cleanup();
                bc = null;
            }
        }

        public void prepare() {
        }
        
        @Override
        protected void playImpl() {
            
            if (isVideo && nativePlayerMode) {
                // To simulate native player mode, we will show a form with the player.
                final Form currForm = Display.getInstance().getCurrent();
                Form playerForm = new Form("Video Player", new com.codename1.ui.layouts.BorderLayout()) {

                    @Override
                    protected void onShow() {
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                playInternal();
                            }

                        });
                    }
                    
                };
                com.codename1.ui.Toolbar tb = new com.codename1.ui.Toolbar();
                playerForm.setToolbar(tb);
                tb.setBackCommand("Back", new com.codename1.ui.events.ActionListener<com.codename1.ui.events.ActionEvent>() {
                    public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                        pauseInternal();
                        currForm.showBack();
                    }
                });
                
                Component videoComponent = getVideoComponent();
                if (videoComponent.getComponentForm() != null) {
                    videoComponent.remove();
                }
                playerForm.addComponent(com.codename1.ui.layouts.BorderLayout.CENTER, videoComponent);
                playerForm.show();
                return;
                
            }
            playInternal();
            
        }

        private void playInternal() {
            if (bc == null) {
                return;
            }
            bc.execute("cn1Media.play()");
            
        }
        
        private void pauseInternal() {
            if (bc == null) {
                return;
            }
            bc.execute("cn1Media.pause()");
            
            
        
        }
        
        
        
        
        @Override
        protected void pauseImpl() {
            if (bc == null) {
                return;
            }
            if(playing) {
                bc.execute("cn1Media.pause()");
            }
            //playing = false;
            
        }

        public int getTime() {
            if (bc == null) {
                return -1;
            }
            return currTime;
        }

        public void setTime(final int time) {
            if (bc == null) {
                return;
            }
            bc.execute("cn1Media.currentTime = "+(time/1000.0)+";");
           
            
        }

        public int getDuration() {
            int d = duration;
            if(d == 0){
                return -1;
            }
            return d;
        }

        public void setVolume(int vol) {
            if (bc == null) {
                return;
            }
            bc.execute("cn1Media.volume = "+((double) vol / 100d));
        }

        public int getVolume() {
            return volume;
        }


        
        @Override
        public com.codename1.ui.Component getVideoComponent() {
            if (!isVideo) {
                return new Label();
            } else {
                return bc;
            }
           
            
        }

        public boolean isVideo() {
            return isVideo;
        }

        public boolean isFullScreen() {
            return false;
        }

        public void setFullScreen(boolean fullScreen) {
        }

        @Override
        public boolean isPlaying() {
            return playing;
        }

        @Override
        public void setNativePlayerMode(boolean nativePlayer) {
            nativePlayerMode = nativePlayer;
        }

        @Override
        public boolean isNativePlayerMode() {
            return nativePlayerMode;
        }

        public void setVariable(String key, Object value) {
        }

        public Object getVariable(String key) {
            return null;
        }
    }

    @Override
    public AsyncResource<Media> createMediaAsync(InputStream inputStream, String mimeType, Runnable onCompletion) {
        final AsyncResource<Media> out = new AsyncResource<Media>();

        if(!checkForPermission("android.permission.READ_PHONE_STATE", "This is required to play media")){
            out.error(new IOException("android.permission.READ_PHONE_STATE is required to play media"));
            return out;
        }
        if(!checkForPermission("android.permission.WRITE_EXTERNAL_STORAGE", "This is required to play media")){
            out.error(new IOException("android.permission.WRITE_EXTERNAL_STORAGE is required to play media"));
            return out;
        }
        
        java.awt.Container cnt = canvas.getParent();
        while (!(cnt instanceof JFrame)) {
            cnt = cnt.getParent();
            if (cnt == null) {
                out.error(new RuntimeException("Could not find canvas.  Cannot create media"));
                return out;
            }
        }
        
        StreamWrapper stream = new StreamWrapper(inputStream, mimeType , -1);
        String id = BrowserPanel.getStreamRegistry().registerStream(stream);
        String uriAddress = "https://cn1app/streams/"+id;
        try {
            new CodenameOneMediaPlayer(uriAddress, mimeType.startsWith("video/"), (JFrame)cnt, onCompletion, out);
        } catch (IOException ex) {
            out.error(ex);
        }
        return out;
        
    }
    
    
    

    @Override
    public AsyncResource<Media> createMediaAsync(String uriAddress, boolean isVideo, Runnable onCompletion) {
        final AsyncResource<Media> out = new AsyncResource<Media>();

        if(!checkForPermission("android.permission.READ_PHONE_STATE", "This is required to play media")){
            out.error(new IOException("android.permission.READ_PHONE_STATE is required to play media"));
            return out;
        }
        if(!checkForPermission("android.permission.WRITE_EXTERNAL_STORAGE", "This is required to play media")){
            out.error(new IOException("android.permission.WRITE_EXTERNAL_STORAGE is required to play media"));
            return out;
        }
        
        java.awt.Container cnt = canvas.getParent();
        while (!(cnt instanceof JFrame)) {
            cnt = cnt.getParent();
            if (cnt == null) {
                out.error(new RuntimeException("Could not find canvas.  Cannot create media"));
                return out;
            }
        }
        
        if(uriAddress.startsWith("file:")) {
            uriAddress = unfile(uriAddress);
            File f = new File(uriAddress);
            
            if (f.exists()) {
                
                try {
                    FileInputStream fis = new FileInputStream(f);
                    StreamWrapper stream = new StreamWrapper(fis, getMimetype(f), f.length());
                    String id = BrowserPanel.getStreamRegistry().registerStream(stream);
                    //uriAddress = InputStreamSchemeHandler.getURL(id);
                    uriAddress = "https://cn1app/streams/"+id;
                    //new CodenameOneMediaPlayer(stream, ((JFrame)cnt), onCompletion, out);
                } catch (IOException ex) {
                    out.error(ex);
                    return out;
                }
                //uriAddress = "file://"+f.getAbsolutePath().replace('\\', '/');
                
                
            } else {
                out.error(new FileNotFoundException(uriAddress));
                return out;
            }
        }
        
        
        final String uri = uriAddress;
        
        
        try {
            new CodenameOneMediaPlayer(uri, isVideo, (JFrame)cnt, onCompletion, out);
        } catch (IOException ex) {
            out.error(ex);
        }
        return out;
    }

    @Override
    public void addCompletionHandler(Media media, Runnable onCompletion) {
        super.addCompletionHandler(media, onCompletion);
        if (media instanceof CodenameOneMediaPlayer) {
            ((CodenameOneMediaPlayer)media).addCompletionHandler(onCompletion);
        }
    }

    @Override
    public void removeCompletionHandler(Media media, Runnable onCompletion) {
        super.removeCompletionHandler(media, onCompletion); 
        if (media instanceof CodenameOneMediaPlayer) {
            ((CodenameOneMediaPlayer)media).removeCompletionHandler(onCompletion);
        }
    }
    
    
    
}
