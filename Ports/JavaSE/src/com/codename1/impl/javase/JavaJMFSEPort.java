/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase;

import com.codename1.io.Log;
import com.codename1.media.AbstractMedia;
import com.codename1.media.AsyncMedia;
import com.codename1.media.Media;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.PeerComponent;
import com.codename1.util.AsyncResource;
import java.awt.EventQueue;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.AudioDeviceUnavailableEvent;
import javax.media.ConnectionErrorEvent;
import javax.media.Controller;
import javax.media.ControllerAdapter;
import javax.media.ControllerErrorEvent;
import javax.media.DataLostErrorEvent;
import javax.media.EndOfMediaEvent;
import javax.media.InternalErrorEvent;
import javax.media.Player;
import javax.media.ResourceUnavailableEvent;
import javax.media.StartEvent;
import javax.media.StopEvent;
import javax.media.Time;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * A "fall-back" JavaSE implementation that does not support a native browser component, and uses JMF for media.
 * This is only used if CEF and FX are both not found.
 * @author shannah
 */
public class JavaJMFSEPort extends JavaSEPort {

    @Override
    public AsyncResource<Media> createMediaAsync(final InputStream stream, final String mimeType, final Runnable onCompletion) {
        final AsyncResource<Media> out = new AsyncResource<Media>();
        java.awt.Container cnt = canvas.getParent();
        while (!(cnt instanceof JFrame)) {
            cnt = cnt.getParent();
            if (cnt == null) {
                out.error(new RuntimeException("Could not find canvas.  Cannot create media"));
                return out;
            }
        }
        final java.awt.Container fCnt = cnt;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    out.complete(new CodenameOneMediaPlayer(stream, mimeType, (JFrame)fCnt, onCompletion, out));
                } catch (IOException ex) {
                    out.error(ex);
                }
            }
        });
        return out;
    }

    @Override
    public AsyncResource<Media> createMediaAsync(final String uriAddress, final boolean isVideo, final Runnable onCompletion) {
        final AsyncResource<Media> out = new AsyncResource<Media>();
        java.awt.Container cnt = canvas.getParent();
        while (!(cnt instanceof JFrame)) {
            cnt = cnt.getParent();
            if (cnt == null) {
                out.error(new RuntimeException("Could not find canvas.  Cannot create media"));
                return out;
            }
        }
        final java.awt.Container fCnt = cnt;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    out.complete(new CodenameOneMediaPlayer(uriAddress, isVideo, (JFrame)fCnt, onCompletion, out));
                } catch (IOException ex) {
                    out.error(ex);
                }
            }
        });
        return out;
    }

    @Override
    public PeerComponent createBrowserComponent(Object browserComponent) {
        return null;
    }

    @Override
    public boolean isNativeBrowserComponentSupported() {
        return false;
    }
    
    
    class CodenameOneMediaPlayer extends AbstractMedia {
        private java.util.Timer endMediaPoller;
        private Runnable onCompletion;
        private java.util.List<Runnable> completionHandlers;
        private Player player;
//        private MediaPlayer player;
        private boolean realized = false;
        private boolean isVideo;
        
        private JFrame frm;
        private boolean playing = false;
        private boolean nativePlayerMode;
        private AsyncResource<Media> _callback;
        
        /**
         * This is a callback for the JMF media player that is supposed to fire
         * when the media is paused. 
         */
        private Runnable onPaused = new Runnable() {
            public void run() {
                if (endMediaPoller != null) {
                    endMediaPoller.cancel();
                    endMediaPoller = null;
                }
                stopEndMediaPoller();
                playing = false;

                fireMediaStateChange(AsyncMedia.State.Paused);
            }
        };
        /**
         * This is a callback for the JMF media player that is supposed to fire
         * when the media is paused.
         */
        private Runnable onPlaying = new Runnable() {
            @Override
            public void run() {
                playing = true;
                startEndMediaPoller();
                fireMediaStateChange(AsyncMedia.State.Playing);
            }
            
        };
        
        private String lastErrorMessage;
        private AsyncMedia.MediaErrorType lastErrorType;
        /**
         * This is a callback for the JMF media player that is supposed to fire
         * when the media is paused.
         */
        private Runnable onError = new Runnable() {
            public void run() {
                if (_callback != null && !_callback.isDone()) {
                    _callback.error(createMediaException(lastErrorMessage, lastErrorType));
                    return;
                } else {
                    Log.p(lastErrorMessage);
                }
                fireMediaError(createMediaException(lastErrorMessage, lastErrorType));
                if (!playing) {
                    stopEndMediaPoller();
                    fireMediaStateChange(AsyncMedia.State.Playing);
                    fireMediaStateChange(AsyncMedia.State.Paused);
                }

            }
        };
        
        
        private boolean isPlayable(String uri) {
            return true;
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
            init(temp.toURI().toString(), mimeType.contains("video"), f, onCompletion, callback);

        }

        
        public CodenameOneMediaPlayer(String uri, boolean isVideo, JFrame f, final Runnable onCompletion, final AsyncResource<Media> callback) throws IOException {
            init(uri, isVideo, f, onCompletion, callback);
        }
        
        private void init(String uri, boolean isVideo, JFrame f, final Runnable onCompletion, final AsyncResource<Media> callback) throws IOException {
            frm = f;
            _callback = callback;
            if (onCompletion != null) {
                addCompletionHandler(onCompletion);
            }
            
            this.onCompletion = new Runnable() {

                @Override
                public void run() {
                    if (callback != null && !callback.isDone()) {
                        callback.complete(CodenameOneMediaPlayer.this);
                    }
                    stopEndMediaPoller();
                    playing = false;
                    
                    fireMediaStateChange(AsyncMedia.State.Paused);
                    fireCompletionHandlers();
                }
                
            };
            this.isVideo = isVideo;
            this.frm = f;
            try {
                if (uri.startsWith("file:")) {
                    uri = unfile(uri);
                }
                File fff = new File(uri);
                if(fff.exists()) {
                    uri = fff.toURI().toURL().toExternalForm();
                }
                if (isVideo && !isPlayable(uri)) {

                    File temp = File.createTempFile("mtmp", ".mp4");
                    temp.deleteOnExit();
                    FileOutputStream out = new FileOutputStream(temp);
                    byte buf[] = new byte[1024];
                    int len = 0;
                    InputStream stream = new URL(uri).openStream();
                    while ((len = stream.read(buf, 0, buf.length)) > -1) {
                        out.write(buf, 0, len);
                    }
                    stream.close();
                    uri = temp.toURI().toURL().toExternalForm();
                }
                
                player = javax.media.Manager.createRealizedPlayer(new java.net.URL(uri));
                player.addControllerListener(new ControllerAdapter() {
                    @Override
                    public void endOfMedia(EndOfMediaEvent e) {
                        System.out.println("Reached end of media");
                        CodenameOneMediaPlayer.this.onCompletion.run();
                    }

                    @Override
                    public void start(StartEvent e) {
                        System.out.println("In start event");
                        onPlaying.run();
                    }

                    @Override
                    public void stop(StopEvent e) {
                        System.out.println("In stop event");
                        onPaused.run();
                    }
                    
                    

                    @Override
                    public void connectionError(ConnectionErrorEvent e) {
                        lastErrorType = MediaErrorType.Network;
                        lastErrorMessage = e.getMessage();
                        onError.run();
                    }

                    @Override
                    public void dataLostError(DataLostErrorEvent e) {
                        lastErrorMessage = e.getMessage();
                        lastErrorType = MediaErrorType.Network;
                        onError.run();
                    }

                    @Override
                    public void internalError(InternalErrorEvent e) {
                        lastErrorMessage = e.getMessage();
                        lastErrorType = MediaErrorType.Unknown;
                    }

                    @Override
                    public void controllerError(ControllerErrorEvent e) {
                        lastErrorMessage = e.getMessage();
                        lastErrorType = MediaErrorType.Unknown;
                    }

                    @Override
                    public void resourceUnavailable(ResourceUnavailableEvent e) {
                        lastErrorMessage = e.getMessage();
                        lastErrorType = MediaErrorType.LineUnavailable;
                    }

                    @Override
                    public void audioDeviceUnavailable(AudioDeviceUnavailableEvent e) {
                        lastErrorMessage = "Audio device unavailable";
                        lastErrorType = MediaErrorType.LineUnavailable;
                    }
                    
                    
                    
                    
                    
                    
                    
                    
                    
                    
                    
                    
                    
                    
                    
                });
               
                

            } catch (Exception ex) {
                if (callback != null && !callback.isDone()) {
                    callback.error(ex);
                } else {
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                }
            }
        }

        private com.codename1.media.AsyncMedia.MediaException createMediaException(String message, AsyncMedia.MediaErrorType type) {
            
            return new com.codename1.media.AsyncMedia.MediaException(type, message);
        }
        
        /**
         * This starts a timer which checks the status of media every Xms so that it can fire
         * status change events and on completion events.  The JavaFX media player has onPlaying,
         * onPaused, etc.. status events of its own that seem to not work in many cases, so we
         * need to use this timer to poll for the status.  That really sucks!!
         */
        private void startEndMediaPoller() {
            stopEndMediaPoller();
            endMediaPoller = new java.util.Timer();
            endMediaPoller.schedule(new TimerTask() {
                @Override
                public void run() {
                    
                    // Check if the media is playing but we haven't updated our status.
                    // If so, we change our status to playing, and fire a state change event.
                    if (!playing && player.getState() == Controller.Started) {
                        EventQueue.invokeLater(new Runnable() {
                            // State tracking on the fx thread to avoid race conditions.
                            public void run() {
                                if (!playing && player.getState() == Controller.Started) {
                                    playing = true;
                                    fireMediaStateChange(AsyncMedia.State.Playing);
                                }
                            }
                            
                        });
                        
                    } else if (playing && player.getState() != Controller.Started) {
                        stopEndMediaPoller();
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                if (playing && player.getState() != Controller.Started) {
                                    
                                    playing = false;
                                    fireMediaStateChange(AsyncMedia.State.Paused);
                                }
                            }
                            
                        });
                    }
                    double diff = player.getDuration().getNanoseconds()/1_000_000l- player.getMediaTime().getNanoseconds()/1_000_000l;
                    if (playing && diff < 0.01) {
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                double diff = player.getDuration().getNanoseconds()/1_000_000l- player.getMediaTime().getNanoseconds()/1_000_000l;
                                if (playing && diff < 0.01) {
                                    Runnable completionCallback = CodenameOneMediaPlayer.this.onCompletion;
                                    if (completionCallback != null) {
                                        completionCallback.run();
                                    }
                                }
                            }
                        });
                    }
                }

            }, 100, 100);
        }
        
        /**
         * Stop the media state poller. This is called when the media is paused.
         */
        private void stopEndMediaPoller() {
            if (endMediaPoller != null) {
                endMediaPoller.cancel();
                endMediaPoller = null;
            }
        }
        
        
        private void fireCompletionHandlers() {
            if (completionHandlers != null && !completionHandlers.isEmpty()) {
                
                CN.callSerially(new Runnable() {

                    @Override
                    public void run() {
                        if (completionHandlers != null && !completionHandlers.isEmpty()) {
                            List<Runnable>  toRun;

                            synchronized(CodenameOneMediaPlayer.this) {
                                toRun = new ArrayList<Runnable>(completionHandlers);
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
                    completionHandlers = new ArrayList<Runnable>();
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
            player.deallocate();
        }

        public void prepare() {
            player.prefetch();
        }
        
        @Override
        protected void playImpl() {
            
            if (isVideo && nativePlayerMode) {
                // To simulate native player mode, we will show a form with the player.
                final Form currForm = CN.getCurrentForm();
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
            
            if (player.getState() != Controller.Started) {
                player.start();
                startEndMediaPoller();
            }
        }
        
        private void pauseInternal() {
            if (player.getState() == Controller.Started) {
                player.stop();
                stopEndMediaPoller();
            }
        }
        
        
        @Override
        protected void pauseImpl() {
            //if(player.getStatus() == Status.PLAYING) {
            if (player.getState() == Controller.Started) {
                pauseInternal();
            }
            //playing = false;
            
        }

        public int getTime() {
            return (int)(player.getMediaTime().getNanoseconds() / 1_000_000l);
            
        }

        public void setTime(final int time) {
            player.setMediaTime(new Time(time * 1_000_000l));
           
            
        }

        public int getDuration() {
            int d = (int) (player.getDuration().getNanoseconds() / 1_000_000l);
            if(d == 0){
                return -1;
            }
            return d;
        }

        public void setVolume(int vol) {
            player.getGainControl().setLevel(((float) vol / 100f));
            
        }

        public int getVolume() {
            return (int) player.getGainControl().getLevel() * 100;
        }

        private Component videoComponent;
        
        @Override
        public Component getVideoComponent() {
            if (videoComponent != null) {
                return videoComponent;
            }
            if (!isVideo) {
                return new Label();
            }
            final java.awt.Component awtComponent = player.getVisualComponent();
            if (awtComponent != null) {
                if (videoComponent == null) {
                    EventQueue.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            videoComponent = new Peer(frm, awtComponent);
                        }
                    });
                    
                    CN.invokeAndBlock(new Runnable() {

                        @Override
                        public void run() {
                            while (videoComponent == null) {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(JavaJMFSEPort.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    });
                }
                return videoComponent;
            }
            System.out.println("Video Playing is not supported on this platform");
            Label l = new Label("Video");
            l.getStyle().setAlignment(Component.CENTER);
            return l;
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
    public void addCompletionHandler(Media media, Runnable onCompletion) {
        super.addCompletionHandler(media, onCompletion);
        if (media instanceof CodenameOneMediaPlayer) {
            ((CodenameOneMediaPlayer)media).addCompletionHandler(onCompletion);
        }
    }
    
    
}
