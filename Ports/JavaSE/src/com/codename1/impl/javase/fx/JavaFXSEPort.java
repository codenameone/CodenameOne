/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase.fx;

import com.codename1.impl.javase.AbstractBrowserWindowSE;
import com.codename1.impl.javase.BrowserWindowFactory;
import com.codename1.impl.javase.IBrowserComponent;
import com.codename1.impl.javase.JavaSEPort;
import static com.codename1.impl.javase.JavaSEPort.checkForPermission;
import static com.codename1.impl.javase.JavaSEPort.retinaScale;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.media.AbstractMedia;
import com.codename1.media.AsyncMedia;
import com.codename1.media.Media;
import com.codename1.ui.Accessor;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.PeerComponent;
import com.codename1.util.AsyncResource;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.media.MediaView;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author shannah
 */
public class JavaFXSEPort extends JavaSEPort {

    @Override
    public void init(Object m) {
        super.init(m);
        try {
            Class.forName("javafx.embed.swing.JFXPanel");
            Platform.setImplicitExit(false);
            fxExists = true;
        } catch (Throwable ex) {
        }
    }
    
    private static boolean isPlayable(String filename) {
        try {
            javafx.scene.media.Media media = new javafx.scene.media.Media(filename);
        } catch (javafx.scene.media.MediaException e) {
            if (e.getType() == javafx.scene.media.MediaException.Type.MEDIA_UNSUPPORTED) {
                return false;
            }
        }
        return true;
    }
    
    
     @Override
    public AsyncResource<Media> createMediaAsync(String uriAddress, final boolean isVideo, final Runnable onCompletion) {
        
        final AsyncResource<Media> out = new AsyncResource<Media>();
        if(!checkForPermission("android.permission.READ_PHONE_STATE", "This is required to play media")){
            out.error(new IOException("android.permission.READ_PHONE_STATE is required to play media"));
            return out;
        }
        if(!checkForPermission("android.permission.WRITE_EXTERNAL_STORAGE", "This is required to play media")){
            out.error(new IOException("android.permission.WRITE_EXTERNAL_STORAGE is required to play media"));
            return out;
        }
        
        if(uriAddress.startsWith("file:")) {
            uriAddress = unfile(uriAddress);
        }
        final String uri = uriAddress;
        if (!fxExists) {
            String msg = "This fetaure is supported from Java version 1.7.0_06, update your Java to enable this feature. This might fail on OpenJDK as well in which case you will need to install the Oracle JDK. ";
            System.out.println(msg);
            out.error(new IOException(msg));
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

        final java.awt.Container c = cnt;

        //final Media[] media = new Media[1];
        final Exception[] err = new Exception[1];
        final javafx.embed.swing.JFXPanel m = new CN1JFXPanel();
        //mediaContainer = m;
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                try {
                    if (uri.indexOf(':') < 0 && uri.lastIndexOf('/') == 0) {
                        String mimeType = "video/mp4";
                        new CodenameOneMediaPlayer(getResourceAsStream(getClass(), uri), mimeType, (JFrame) c, m, onCompletion, out);
                        return;
                    }

                   new CodenameOneMediaPlayer(uri, isVideo, (JFrame) c, m, onCompletion, out);
                } catch (Exception ex) {
                    out.error(ex);
                }
            }
        });
        return out;
    }
    
     /**
     * Plays the sound in the given stream
     *
     * @param stream the stream containing the media data
     * @param mimeType the type of the data in the stream
     * @param onCompletion invoked when the audio file finishes playing, may be
     * null
     * @return a handle that can be used to control the playback of the audio
     * @throws java.io.IOException if the URI access fails
     */
    @Override
    public AsyncResource<Media> createMediaAsync(final InputStream stream, final String mimeType, final Runnable onCompletion) {
        final AsyncResource<Media> out = new AsyncResource<Media>();
        if(!checkForPermission("android.permission.READ_PHONE_STATE", "This is required to play media")){
            out.error(new IOException("android.permission.READ_PHONE_STATE is required to play media"));
            return out;
        }
        if(!checkForPermission("android.permission.WRITE_EXTERNAL_STORAGE", "This is required to play media")){
             out.error(new IOException("android.permission.WRITE_EXTERNAL_STORAGE is required to play media"));
            return out;
        }
        
        if (!fxExists) {
            String msg = "This fetaure is supported from Java version 1.7.0_06, update your Java to enable this feature. This might fail on OpenJDK as well in which case you will need to install the Oracle JDK. ";
            //System.out.println(msg);
            out.error(new IOException(msg));
            return out;
        }
        java.awt.Container cnt = canvas.getParent();
        while (!(cnt instanceof JFrame)) {
            cnt = cnt.getParent();
            if (cnt == null) {
                return null;
            }
        }
        final java.awt.Container c = cnt;

        //final Media[] media = new Media[1];
        //final Exception[] err = new Exception[1];
        final javafx.embed.swing.JFXPanel m = new CN1JFXPanel();
        //mediaContainer = m;

        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                try {
                    new CodenameOneMediaPlayer(stream, mimeType, (JFrame) c, m, onCompletion, out);
                } catch (Exception ex) {
                    out.error(ex);
                }
            }
        });

        return out;
    }
    
    
    class CodenameOneMediaPlayer extends AbstractMedia {
        private java.util.Timer endMediaPoller;
        private Runnable onCompletion;
        private List<Runnable> completionHandlers;
        private javafx.scene.media.MediaPlayer player;
//        private MediaPlayer player;
        private boolean realized = false;
        private boolean isVideo;
        private javafx.embed.swing.JFXPanel videoPanel;
        private JFrame frm;
        private boolean playing = false;
        private boolean nativePlayerMode;
        private AsyncResource<Media> _callback;
        
        /**
         * This is a callback for the JavaFX media player that is supposed to fire
         * when the media is paused.  Unfortunately this is unreliable as the status
         * events seem to stop working after the first time it is paused.  
         * We use a poller (the endMediaPoller timer) to track the status of the video
         * so that the change listeners are fired.  This really sucks!
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
                startEndMediaPoller();
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
                    _callback.error(player.errorProperty().get());
                    return;
                } else {
                    Log.e(player.errorProperty().get());
                }
                fireMediaError(createMediaException(player.errorProperty().get()));
                if (!playing) {
                    stopEndMediaPoller();
                    fireMediaStateChange(AsyncMedia.State.Playing);
                    fireMediaStateChange(AsyncMedia.State.Paused);
                }

            }
        };
        
        public CodenameOneMediaPlayer(String uri, boolean isVideo, JFrame f, javafx.embed.swing.JFXPanel fx, final Runnable onCompletion, final AsyncResource<Media> callback) throws IOException {
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
                    // JavaFX doesn't seem to support .mov files.  But if you simply rename it
                    // to .mp4, then it will play it  (if it is mpeg4).
                    // So this will improve .mov files from failing to 100% of the time
                    // to only half of the time (when it isn't an mp4)
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
                
                player = new MediaPlayer(new javafx.scene.media.Media(uri));
                
                player.setOnReady(new Runnable() {
                    public void run() {
                        if (callback != null && !callback.isDone()) {
                            callback.complete(CodenameOneMediaPlayer.this);
                        }
                    }
                });

                installFxCallbacks();
                if (isVideo) {
                    videoPanel = fx;
                }
                

            } catch (Exception ex) {
                if (callback != null && !callback.isDone()) {
                    callback.error(ex);
                } else {
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                }
            }
        }

        private com.codename1.media.AsyncMedia.MediaException createMediaException(javafx.scene.media.MediaException ex) {
            AsyncMedia.MediaErrorType type;
            switch (ex.getType()) {
                case MEDIA_CORRUPTED:
                    type = AsyncMedia.MediaErrorType.Decode;
                    break;
                case MEDIA_INACCESSIBLE:
                case MEDIA_UNAVAILABLE:
                    type = AsyncMedia.MediaErrorType.Network;
                    break;
                case MEDIA_UNSUPPORTED:
                    type = AsyncMedia.MediaErrorType.SrcNotSupported;
                    break;
                case MEDIA_UNSPECIFIED:
                    type = AsyncMedia.MediaErrorType.Unknown;
                    break;
                case OPERATION_UNSUPPORTED:
                    type = AsyncMedia.MediaErrorType.SrcNotSupported;
                    break;
                case PLAYBACK_ERROR:
                    type = AsyncMedia.MediaErrorType.Decode;
                    break;
                case PLAYBACK_HALTED:
                    type = AsyncMedia.MediaErrorType.Aborted;
                    break;
                //case UNKNOWN:
                default:
                    type = AsyncMedia.MediaErrorType.Unknown;
                    break;
                    
            }
            return new com.codename1.media.AsyncMedia.MediaException(type, ex);
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
                    if (!playing && player.getStatus() == MediaPlayer.Status.PLAYING) {
                        Platform.runLater(new Runnable() {
                            // State tracking on the fx thread to avoid race conditions.
                            public void run() {
                                if (!playing && player.getStatus() == MediaPlayer.Status.PLAYING) {
                                    playing = true;
                                    fireMediaStateChange(AsyncMedia.State.Playing);
                                }
                            }
                            
                        });
                        
                    } else if (playing && player.getStatus() != MediaPlayer.Status.PLAYING) {
                        stopEndMediaPoller();
                        Platform.runLater(new Runnable() {
                            public void run() {
                                if (playing && player.getStatus() != MediaPlayer.Status.PLAYING) {
                                    
                                    playing = false;
                                    fireMediaStateChange(AsyncMedia.State.Paused);
                                }
                            }
                            
                        });
                    }
                    double diff = player.getTotalDuration().toMillis() - player.getCurrentTime().toMillis();
                    if (playing && diff < 0.01) {
                        Platform.runLater(new Runnable() {
                            public void run() {
                                double diff = player.getTotalDuration().toMillis() - player.getCurrentTime().toMillis();
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
        
        public CodenameOneMediaPlayer(InputStream stream, String mimeType, JFrame f, javafx.embed.swing.JFXPanel fx, final Runnable onCompletion, final AsyncResource<Media> callback) throws IOException {
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
            this.isVideo = mimeType.contains("video");
            this.frm = f;
            try {
                player = new MediaPlayer(new javafx.scene.media.Media(temp.toURI().toString()));
                player.setOnReady(new Runnable() {
                    public void run() {
                        if (callback != null) {
                            callback.complete(CodenameOneMediaPlayer.this);
                        }
                    }
                });
                installFxCallbacks();
                if (isVideo) {
                    videoPanel = fx;
                }
                

            } catch (Exception ex) {
                if (callback != null) {
                    callback.error(ex);
                } else {
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                }
            }
        }

        
        private void fireCompletionHandlers() {
            if (completionHandlers != null && !completionHandlers.isEmpty()) {
                
                Display.getInstance().callSerially(new Runnable() {

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
                        Platform.runLater(new Runnable() {
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
            
            installFxCallbacks();
            player.play();
            startEndMediaPoller();
        }
        
        private void pauseInternal() {
            player.pause();
            
            
        
        }
        
        /**
         * Installs listeners for the javafx media player.  Unfortunately these are
         * incredibly unreliable.  onPlaying only first the first time it plays.  OnPaused
         * also stops firing after the first pause.  onEndOfMedia sometimes fires but not
         * other times.  We use the endOfMediaPoller timer as a backup to test for status
         * changes.
         */
        private void installFxCallbacks() {
            
            player.setOnPlaying(onPlaying);
            player.setOnPaused(onPaused);
            player.setOnError(onError);
            player.setOnEndOfMedia(onCompletion);
           
        }
        
        @Override
        protected void pauseImpl() {
            if(player.getStatus() == Status.PLAYING) {
                pauseInternal();
            }
            //playing = false;
            
        }

        public int getTime() {
            return (int) player.getCurrentTime().toMillis();
        }

        public void setTime(final int time) {
           player.seek(new Duration(time));
            
        }

        public int getDuration() {
            int d = (int) player.getStopTime().toMillis();
            if(d == 0){
                return -1;
            }
            return d;
        }

        public void setVolume(int vol) {
            player.setVolume(((double) vol / 100d));
        }

        public int getVolume() {
            return (int) player.getVolume() * 100;
        }

        @Override
        public Component getVideoComponent() {
            if (!isVideo) {
                return new Label();
            }
            if (videoPanel != null) {
                final Component[] retVal = new Component[1];
                Platform.runLater(new Runnable() {

                    @Override
                    public void run() {
                        retVal[0] = new VideoComponent(frm, videoPanel, player);
                    }
                });
                Display.getInstance().invokeAndBlock(new Runnable() {

                    @Override
                    public void run() {
                        while (retVal[0] == null) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(JavaSEPort.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                });
                return retVal[0];
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

    @Override
    public void removeCompletionHandler(Media media, Runnable onCompletion) {
        super.removeCompletionHandler(media, onCompletion); 
        if (media instanceof CodenameOneMediaPlayer) {
            ((CodenameOneMediaPlayer)media).removeCompletionHandler(onCompletion);
        }
    }

     /**
     * Video peer component.
     * 
     * In contrast to the BrowserComponent and Peer (other peer components),
     * the native peer supports hi-resolution retina displays.  This was possible
     * on this component, but no browser component, or other components in general
     * because videos don't need to respond to pointer events.  Thus the rendered
     * dimensions and location (on the CN1 pipeline) may be different the size and 
     * position of the actual component on the screen (even though in all cases
     * we hide the actual component).
     */
    class VideoComponent extends PeerComponent {

        private javafx.embed.swing.JFXPanel vid;
        private JFrame frm;
        
        // Container that holds the video
        private JPanel cnt = new JPanel();
        private MediaView v;
        private boolean init = false;
        private Rectangle bounds = new Rectangle();
        
        // AWT paints to this buffered image
        // CN1 reads from the buffered image to paint in its own pipeline.
        BufferedImage buf;

        // Gets the buffered image that AWT paints to and CN1 reads from
        private BufferedImage getBuffer() {
            if (buf == null || buf.getWidth() != cnt.getWidth() || buf.getHeight() != cnt.getHeight()) {

                buf = new BufferedImage((int)(cnt.getWidth()), (int)(cnt.getHeight()), BufferedImage.TYPE_INT_ARGB);
            }
            return buf;
        }

        
        
        
        
        /**
         * Paints the native component to the buffer
         */
        private void paintOnBuffer() {
            
            // We need to synchronize on the peer component
            // drawNativePeer will also synchronize on this
            // This prevents simulataneous reads and writes to/from the
            // buffered image.
            if (EventQueue.isDispatchThread()) {
                // Only run this on the AWT event dispatch thread
                // to avoid deadlocks
                synchronized(VideoComponent.this) {
                    paintOnBufferImpl();
                }
            } else if (!Display.getInstance().isEdt()){
                // I can only imagine bad things 
                try {
                    EventQueue.invokeAndWait(new Runnable() {
                        public void run() {
                            paintOnBuffer();
                        }
                    });
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        
        private void paintOnBufferImpl() {
            final BufferedImage buf = getBuffer();
            Graphics2D g2d = buf.createGraphics();
            AffineTransform t = g2d.getTransform();
            double tx = t.getTranslateX();
            double ty = t.getTranslateY();
            vid.paint(g2d);
            g2d.dispose();
            VideoComponent.this.putClientProperty("__buffer", buf);
        }
        
        public VideoComponent(JFrame frm, final javafx.embed.swing.JFXPanel vid, javafx.scene.media.MediaPlayer player) {
            super(null);
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    
                    
                    
                    cnt = new JPanel() {
                        
                        @Override
                        public void paint(java.awt.Graphics g) {
                            paintOnBuffer();
                            
                            // After we paint to the buffer we need to 
                            // tell CN1 to paint the buffer to its pipeline.
                            Display.getInstance().callSerially(new Runnable() {
                                public void run() {
                                    VideoComponent.this.repaint();
                                }
                            });
                            
                        }

                        @Override
                        protected void paintChildren(java.awt.Graphics g) {
                            // Not sure if this is necessary
                            // but we don't want any painting to occur
                            // on regular pipeline
                        }

                        @Override
                        protected void paintBorder(java.awt.Graphics g) {
                            // Not sure if this is necessary but we don't
                            // want any painting to occur on regular pipeline
                        }
                        
                        
                        
                    };

                    cnt.setOpaque(false);
                    vid.setOpaque(false);
                    cnt.setLayout(new BorderLayout());
                    cnt.add(BorderLayout.CENTER, vid);
                    cnt.setVisible(false);
                }
            });

            Group root = new Group();
            
            v = new MediaView(player);
            final Runnable oldOnReady = player.getOnPlaying();
            player.setOnPlaying(new Runnable() {
                public void run() {
                    if (oldOnReady != null) oldOnReady.run();
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            if (VideoComponent.this.getParent() != null) {
                                VideoComponent.this.getParent().revalidate();
                            }
                        }
                    });
                }
            });
            
            root.getChildren().add(v);
            vid.setScene(new Scene(root));

            this.vid = vid;
            this.frm = frm;
            
        }

        @Override
        protected void initComponent() {
            bounds.setBounds(0,0,0,0);
            super.initComponent();
        }

        @Override
        protected void deinitialize() {
            super.deinitialize();
            if (testRecorder != null) {
                testRecorder.dispose();
                testRecorder = null;
            }
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    vid.setScene(null);
                    vid.removeAll();
                    cnt.remove(vid);
                    frm.remove(cnt);
                    frm.repaint();
                    
                    //mediaContainer = null;
                }
            });
            
        }

        protected void setLightweightMode(final boolean l) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {

                    if (!l) {
                        if (!init) {
                            init = true;
                            cnt.setVisible(true);
                            frm.add(cnt, 0);
                            
                            frm.repaint();
                        } else {
                            cnt.setVisible(false);
                        }
                    } else {
                        if (init) {
                            cnt.setVisible(false);
                        }
                    }
                }
            });

        }

        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            com.codename1.ui.geom.Dimension out = new com.codename1.ui.geom.Dimension((int)(vid.getPreferredSize().width), (int)(vid.getPreferredSize().height));
            return out;
        }

        @Override
        public void paint(final Graphics g) {
            if (init) {
                
                onPositionSizeChange();
                drawNativePeer(Accessor.getNativeGraphics(g), this, cnt);
                
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        paintOnBuffer();
                    }
                });
                
                
            }else{
                if(getComponentForm() != null && getComponentForm() == getCurrentForm()){
                    setLightweightMode(false);
                }
            }
        }
        
        
        @Override
        protected void onPositionSizeChange() {
            final int x = getAbsoluteX();
            final int y = getAbsoluteY();
            final int w = getWidth();
            final int h = getHeight();

            int screenX = 0;
            int screenY = 0;
            if(getScreenCoordinates() != null) {
                screenX = getScreenCoordinates().x;
                screenY = getScreenCoordinates().y;
            }
            
            // NOTE:  For the VideoComponent we make the size the actual
            // pixel size of the light-weight peer even though, on retina,
            // this means that the native component is twice the height and
            // width of the native peer.  We can do this here because
            // the video component doesn't have any interactivity 
            // that constrains us to keep the same real position on the screen
            // as our render position is.  THis is not the case for WebBrowser
            // 
            bounds.setBounds((int) ((x + screenX + canvas.x)),
                    (int) ((y + screenY + canvas.y)),
                    (int) (w),
                    (int) (h));
            
            if(!bounds.equals(cnt.getBounds())){
                Platform.runLater(new Runnable() {

                    @Override
                    public void run() {

                        v.setFitWidth(w );
                        v.setFitHeight(h);
                        
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                cnt.setBounds(bounds);
                                cnt.doLayout();
                                paintOnBuffer();
                                
                            }
                        });
                    }
                });
            }

        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public AsyncResource<Image> captureBrowserScreenshot(PeerComponent browserPeer) {
        if (!(browserPeer instanceof SEBrowserComponent)) {
            return null;
        }
        SEBrowserComponent sebc = (SEBrowserComponent)browserPeer;
        return sebc.captureScreenshot();
    }
    
    
    
    public PeerComponent createFXBrowserComponent(final Object parent) {
        java.awt.Container cnt = canvas.getParent();
        while (!(cnt instanceof JFrame)) {
            cnt = cnt.getParent();
            if (cnt == null) {
                return null;
            }
        }
        final java.awt.Container c = cnt;

        final Exception[] err = new Exception[1];
        final javafx.embed.swing.JFXPanel webContainer = new CN1JFXPanel();
        final SEBrowserComponent[] bc = new SEBrowserComponent[1];
        
        

        final SEBrowserComponent bcc = new SEBrowserComponent();
        Platform.setImplicitExit(false);
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                StackPane root = new StackPane();
                final WebView webView = new WebView();
                
                root.getChildren().add(webView);
                webContainer.setScene(new Scene(root));
                
                // now wait for the Swing side to finish initializing f'ing JavaFX is so broken its unbeliveable
                JPanel parentPanel =  ((JPanel)canvas.getParent());
                
                bcc.SEBrowserComponent_init(
                        JavaFXSEPort.this, 
                        parentPanel, 
                        webContainer, 
                        webView, 
                        (BrowserComponent) parent, 
                        hSelector, 
                        vSelector
                );
                
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        bc[0] = bcc;
                        synchronized (bc) {
                            bc.notify();
                        }
                    }
                });
                
            }
        });
        while (bc[0] == null && err[0] == null) {

            Display.getInstance().invokeAndBlock(new Runnable() {

                @Override
                public void run() {
                    Util.wait(bc, 20);
                }
            });
        }
        
        return bc[0];
    }private void browserExposeInJavaScriptImpl(final PeerComponent browserPeer, final Object o, final String name) {
        ((IBrowserComponent) browserPeer).exposeInJavaScript(o, name);
    }
    
    public void browserExposeInJavaScript(final PeerComponent browserPeer, final Object o, final String name) {
        if (!(browserPeer instanceof SEBrowserComponent)) {
            return;
        }
        if (Platform.isFxApplicationThread()) {
            browserExposeInJavaScriptImpl(browserPeer, o, name);
            return;
        }
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                browserExposeInJavaScriptImpl(browserPeer, o, name);
            }
        });
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
        return createFXBrowserComponent(parent);
            
        
    }
    
    public boolean isNativeBrowserComponentSupported() {
        return fxExists && !blockNativeBrowser;
        //return false;
    }
    
    class CN1JFXPanel extends javafx.embed.swing.JFXPanel {

        @Override
        public void revalidate() {
            // We need to override this with an empty implementation to workaround
            // Deadlock bug  http://bugs.java.com/view_bug.do?bug_id=8058870
            // If we allow the default implementation, then it will periodically deadlock
            // when displaying a browser component
        }

        
        
        @Override
        protected void processMouseEvent(MouseEvent e) {
            //super.processMouseEvent(e); //To change body of generated methods, choose Tools | Templates.
            if (!sendToCn1(e)) {
                super.processMouseEvent(e);
            }
            
        }

        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
            if (!sendToCn1(e)) {
                super.processMouseMotionEvent(e); //To change body of generated methods, choose Tools | Templates.
            }
            
        }

        @Override
        protected void processMouseWheelEvent(MouseWheelEvent e) {
            if (!sendToCn1(e)) {
                super.processMouseWheelEvent(e); //To change body of generated methods, choose Tools | Templates.
            }
        }


        
        
        private boolean peerGrabbedDrag=false;
        
        private boolean sendToCn1(MouseEvent e) {
            
            int cn1X = getCN1X(e);
            int cn1Y = getCN1Y(e);
            if ((!peerGrabbedDrag || true) && Display.isInitialized()) {
                Form f = Display.getInstance().getCurrent();
                if (f != null) {
                    Component cmp = f.getComponentAt(cn1X, cn1Y);
                    //if (!(cmp instanceof PeerComponent) || cn1GrabbedDrag) {
                        // It's not a peer component, so we should pass the event to the canvas
                        e = SwingUtilities.convertMouseEvent(this, e, canvas);
                        switch (e.getID()) {
                            case MouseEvent.MOUSE_CLICKED:
                                canvas.mouseClicked(e);
                                break;
                            case MouseEvent.MOUSE_DRAGGED:
                                canvas.mouseDragged(e);
                                break;
                            case MouseEvent.MOUSE_MOVED:
                                canvas.mouseMoved(e);
                                break;
                            case MouseEvent.MOUSE_PRESSED:
                                // Mouse pressed in native component - passed to lightweight cmp
                                if (!(cmp instanceof PeerComponent)) {
                                    cn1GrabbedDrag = true;
                                }
                                canvas.mousePressed(e);
                                break;
                            case MouseEvent.MOUSE_RELEASED:
                                cn1GrabbedDrag = false;
                                canvas.mouseReleased(e);
                                break;
                            case MouseEvent.MOUSE_WHEEL:
                                canvas.mouseWheelMoved((MouseWheelEvent)e);
                                break;
                                
                        }
                        //return true;
                        if (cn1GrabbedDrag) {
                            return true;
                        }
                        if (cmp instanceof PeerComponent) {
                            return false;
                        }
                        return true;
                    //}
                }
            }
            if (e.getID() == MouseEvent.MOUSE_RELEASED) {
                cn1GrabbedDrag = false;
                peerGrabbedDrag = false;
            } else if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                peerGrabbedDrag = true;
            }
            return false;
        }
        
        private int getCN1X(MouseEvent e) {
            if (canvas == null) {
                int out = e.getXOnScreen();
                if (out == 0) {
                    // For some reason the web browser would return 0 for screen coordinates
                    // but would still have correct values for getX() and getY() when 
                    // dealing with mouse wheel events.  In these cases we need to 
                    // get the screen coordinate from the component
                    // and add it to the relative coordinate.
                    out = e.getX(); // In some cases absX is set to zero for mouse wheel events
                    Object source = e.getSource();
                    if (source instanceof java.awt.Component) {
                        Point pt = ((java.awt.Component)source).getLocationOnScreen();
                        out  += pt.x;
                    }
                }
                return out;
            }
            java.awt.Rectangle screenCoords = getScreenCoordinates();
            if (screenCoords == null) {
                screenCoords = new java.awt.Rectangle(0, 0, 0, 0);
            }
            int x = e.getXOnScreen();
            if (x == 0) {
                // For some reason the web browser would return 0 for screen coordinates
                // but would still have correct values for getX() and getY() when 
                // dealing with mouse wheel events.  In these cases we need to 
                // get the screen coordinate from the component
                // and add it to the relative coordinate.
                x = e.getX();
                Object source = e.getSource();
                if (source instanceof java.awt.Component) {
                    Point pt = ((java.awt.Component)source).getLocationOnScreen();
                    x += pt.x;
                }
            }
            return (int)((x - canvas.getLocationOnScreen().x - (canvas.x + screenCoords.x) * zoomLevel / retinaScale) / zoomLevel * retinaScale);
        }

        private int getCN1Y(MouseEvent e) {
            if (canvas == null) {
                int out = e.getYOnScreen();
                if (out == 0) {
                    // For some reason the web browser would return 0 for screen coordinates
                    // but would still have correct values for getX() and getY() when 
                    // dealing with mouse wheel events.  In these cases we need to 
                    // get the screen coordinate from the component
                    // and add it to the relative coordinate.
                    out = e.getY();
                    Object source = e.getSource();
                    if (source instanceof java.awt.Component) {
                        Point pt = ((java.awt.Component)source).getLocationOnScreen();
                        out  += pt.y;
                    }
                }
                return out;
            }
            java.awt.Rectangle screenCoords = getScreenCoordinates();
            if (screenCoords == null) {
                screenCoords = new java.awt.Rectangle(0, 0, 0, 0);
            }
            int y = e.getYOnScreen();
            if (y == 0) {
                // For some reason the web browser would return 0 for screen coordinates
                // but would still have correct values for getX() and getY() when 
                // dealing with mouse wheel events.  In these cases we need to 
                // get the screen coordinate from the component
                // and add it to the relative coordinate.
                y = e.getY();
                Object source = e.getSource();
                if (source instanceof java.awt.Component) {
                    Point pt = ((java.awt.Component)source).getLocationOnScreen();
                    y += pt.y;
                }
            }
            return (int)((y - canvas.getLocationOnScreen().y - (canvas.y + screenCoords.y) * zoomLevel / retinaScale) / zoomLevel * retinaScale);
        }
        
        public CN1JFXPanel() {
            final CN1JFXPanel panel = this;
            
            /*
            panel.addMouseListener(new MouseListener() {
                
                

                @Override
                public void mouseClicked(MouseEvent e) {
                    sendToCn1(e);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    sendToCn1(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    sendToCn1(e);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    //SEBrowserComponent.this.instance.canvas.mouseE
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            });

            panel.addMouseMotionListener(new MouseMotionListener() {

                @Override
                public void mouseDragged(MouseEvent e) {
                    sendToCn1(e);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    sendToCn1(e);
                }

            });

            panel.addMouseWheelListener(new MouseWheelListener() {

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    sendToCn1(e);
                }

            });
            */
            
        }

        
    }
    
    protected BrowserWindowFactory createBrowserWindowFactory() {
        return new BrowserWindowFactory() {
            @Override
            public AbstractBrowserWindowSE createBrowserWindow(String startURL) {
                return new FXBrowserWindowSE(startURL);
            }

       };
    }
}
