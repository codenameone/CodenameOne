package com.codename1.samples;


import com.codename1.components.SpanLabel;
import com.codename1.io.File;
import static com.codename1.ui.CN.*;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.io.Log;
import com.codename1.ui.Toolbar;
import java.io.IOException;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.io.NetworkEvent;
import com.codename1.media.AsyncMedia;
import com.codename1.media.AudioBuffer;
import com.codename1.media.MediaManager;
import com.codename1.media.MediaRecorderBuilder;
import com.codename1.media.WAVWriter;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Slider;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.MessageEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.util.AsyncResource;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A sample playing Async Media.
 * 
 * <p>
 * This example also includes some message handling to receive Javascript messages to play and pause the app.
 * A sample HTML page that loads this app inside an iframe and includes methods to initate playing and pausing
 * can be seen <a href="https://gist.github.com/shannah/963c2eba9b2291f1270b060ef5e39c4f">here</a></p>
 * 
 */
public class AsyncMediaSample {

    private Form current;
    private Resources theme;

    public void init(Object context) {
        // use two network threads instead of one
        updateNetworkThreadCount(2);

        theme = UIManager.initFirstTheme("/theme");

        // Enable Toolbar on all Forms by default
        Toolbar.setGlobalToolbar(true);

        // Pro only feature
        Log.bindCrashProtection(true);

        addNetworkErrorListener(err -> {
            // prevent the event from propagating
            err.consume();
            if(err.getError() != null) {
                Log.e(err.getError());
            }
            Log.sendLogAsync();
            Dialog.show("Connection Error", "There was a networking error in the connection to " + err.getConnectionRequest().getUrl(), "OK", null);
        });        
    }
    
    public void start() {
        if(current != null){
            current.show();
            return;
        }
        Form hi = new Form("Hi World");
        new Recorder().initUI(hi);
        hi.show();
    }

    public void stop() {
        current = getCurrentForm();
        if(current instanceof Dialog) {
            ((Dialog)current).dispose();
            current = getCurrentForm();
        }
    }
    
    public void destroy() {
    }
    
    private class Recorder {
    private Slider slider = new Slider();
    private Label status = new Label();
    private SpanLabel instructions = new SpanLabel();
    private Button pause = new Button("Pause");
    private Button play = new Button("Play");
    private String fileName = "sound.wav";
    private String bufferPath = "sound.pcm";
    private AudioBuffer audioBuffer;
    private AsyncMedia soundClip;
    private AsyncMedia recorder;
    private MediaRecorderBuilder builder;
    private WAVWriter wavWriter;
    private boolean recording;
    private boolean playing;
    private final Object clipLock = new Object();
    private long recordingCounter;
    private Random random = new Random();
    
    private void initUI(Form f) {
        pause.addActionListener(evt->{
            pause();
            status.setText("Paused");
            pause.setEnabled(false);
            play.setEnabled(true);
            play.setVisible(true);
            pause.setVisible(false);
            instructions.setText("Press play to begin");
            
        });
        
        play.addActionListener(evt->{
            pause.setEnabled(true);
            play.setEnabled(false);
            play.setVisible(false);
            pause.setVisible(true);
            record();
            
        });
        CN.addMessageListener((ActionListener<MessageEvent>)evt->{
            System.out.println("Received message "+evt.getMessage()+" code="+evt.getCode());
            
            
            if (evt.isPromptForAudioPlayer()) {
                System.out.println("Received a prompt for the audio player... audio is ready");
                // This is a prompt that is shown when there is audio ready to play
                // but the user needs to interact.  This is javascript-only to get around
                // restrictions that only allow audio in direct response to user interaction
                
                // We should display some kind of UI to let the user know that the audio is ready
                // and they need to press a button to play it.
                evt.consume();
                CN.callSerially(()-> {
                    MessageEvent.PromptPromise res = evt.getPromptPromise();
                    if (Dialog.show("Audio Ready", "The audio is ready.", "Play", "Cancel")) {
                        res.complete(true);
                    } else {
                        res.complete(false);
                    }
                    return;
                });
                return;
                
            }
            
            if (evt.isPromptForAudioRecorder()) {
                System.out.println("Received prompt for audio recorder.  Recorder is ready");
                // This is a prompt that is shown when there is audio ready to play
                // but the user needs to interact.  This is javascript-only to get around
                // restrictions that only allow audio in direct response to user interaction
                
                // We should display some kind of UI to let the user know that the audio is ready
                // and they need to press a button to play it.
                evt.consume();
                CN.callSerially(()->{
                    MessageEvent.PromptPromise res = evt.getPromptPromise();
                    if (Dialog.show("Microphone Ready", "The microphone is ready.", "Start Recording", "Cancel")) {
                        res.complete(true);
                    } else {
                        res.complete(false);
                    }
                    return;
                });
                return;
                
            }
            
            String message = evt.getMessage();
            if ("play".equals(message)) {
                System.out.println("Received play message");
                pause.setEnabled(true);
                play.setEnabled(false);
                play.setVisible(false);
                pause.setVisible(true);
                record();
                return;
            }
            if ("pause".equals(message)) {
                System.out.println("Received pause message");
                pause();
                status.setText("Paused");
                pause.setEnabled(false);
                play.setEnabled(true);
                play.setVisible(true);
                pause.setVisible(false);
                instructions.setText("Press play to begin");
                return;
            }
        });
        f.setTitle("Test Async Media");
        f.setLayout(new BorderLayout());
        f.add(CENTER, LayeredLayout.encloseIn(pause, play));
        f.add(SOUTH, BorderLayout.south(slider).add(CENTER, status));
        f.add(NORTH, instructions);
        instructions.setText("Press play to begin");
        pause.setVisible(false);
        pause.setEnabled(false);
        slider.setEditable(false);
    }
    
    private void pause() {
        if (soundClip != null) {
            soundClip.cleanup();
            soundClip = null;
        }
        if (recorder != null) {
            recorder.pause();
        }
        recording = false;
        playing = false;
        
    }
    
    private void record() {
        if (recording) {
            return;
        }
        if (soundClip != null) {
            soundClip.cleanup();
            soundClip = null;
        }
        playing = false;
        recording = true;
        if (audioBuffer == null) {
            audioBuffer = MediaManager.getAudioBuffer(bufferPath, true, 4096);
            final float[] floatSamples = new float[audioBuffer.getMaxSize()];
            audioBuffer.addCallback(buf->{
                if (!recording) {
                    return;
                }
                synchronized(clipLock) {
                    if (wavWriter == null) {
                        try {
                            System.out.println("Creating WAVWriter for file "+fileName);
                            wavWriter = new WAVWriter(new File(fileName), buf.getSampleRate(), buf.getNumChannels(), 16);
                        } catch (IOException ex) {
                            Log.e(ex);
                            return;
                        }
                    }
                
                    buf.copyTo(floatSamples);
                    try {
                        wavWriter.write(floatSamples, 0, buf.getSize());
                    } catch (IOException ex) {
                        Log.e(ex);
                    }
                }
            });
        }
        if (builder == null) {
            builder = new MediaRecorderBuilder()
                    .audioChannels(1)
                    .path(bufferPath)
                    .redirectToAudioBuffer(true);
        }
        if (recorder == null) {
            try {
                recorder = MediaManager.getAsyncMedia(MediaManager.createMediaRecorder(builder));
            } catch (IOException ex) {
                Log.e(ex);
                recording = false;
                return;
            }
        }
        synchronized(clipLock) {
            wavWriter = null;
        }
        status.setText("");
        instructions.setText("Please wait");
        Form form = status.getComponentForm();
        Log.p("recorder.playAsync()");
        recorder.playAsync().ready(m->{
            Log.p("recorder.playAsync() ready");
            if (!recording) {
                Log.p("By the time the recorder had started recording, we had already stopped");
                return;
            }
            System.out.println("READY");
            status.setText("Recording");
            instructions.setText("Speak into microphone");
            recordingCounter = 1000l + (long)Math.round(random.nextDouble() * 4000);
            final long startCounter = recordingCounter;
            final long startTime = System.currentTimeMillis();
            slider.setProgress(100);
            final Timer t = new Timer();
            t.schedule(new TimerTask() {
                public void run() {
                    long elapsed = System.currentTimeMillis() - startTime;
                    recordingCounter = startCounter - elapsed;
                    if (recordingCounter <= 0) {
                        t.cancel();
                        CN.callSerially(()->{
                            System.out.println("Recording counter reached zero:  Playing now");
                            if (recording) {
                                play();
                            }
                        });
                    } else {
                        CN.callSerially(()->{
                            if (!recording) {
                                t.cancel();
                                return;
                            }
                            int progress = (int)Math.round(recordingCounter * 100.0 / startCounter);
                            Log.p("progress="+progress);
                            slider.setProgress(progress);
                            Form f = slider.getComponentForm();
                            if (f != null) {
                                f.revalidateWithAnimationSafety();
                            }
                        });
                    }
                }
            }, 200, 200);
            
        }).except(ex->{
            Log.p("Failed to start recorder");
            Log.e(ex);
            
        });
 
    }
    
    private void play() {
        System.out.println("play()");
        if (recording) {
            recording = false;
            recorder.pause();
            System.out.println("Waiting for clipLock");
            synchronized(clipLock) {
                System.out.println("Obtained clipLock");
                if (wavWriter != null) {
                    try {
                        wavWriter.close();
                    } catch (Exception ex) {
                        Log.e(ex);
                    }
                }
                System.out.println("FInished closing wavWriter");
            }
        }
        if (playing) {
            System.out.println("Already playing");
            return;
        }
        playing = true;
        if (soundClip != null) {
            System.out.println("Cleanup up soundClip");
            soundClip.cleanup();
            soundClip = null;
        }
        System.out.println("About to create new soundClip");
        try {
            soundClip = MediaManager.getAsyncMedia(MediaManager.createMedia(new File(fileName).getAbsolutePath(), false));
            MediaManager.addCompletionHandler(soundClip, ()->{
                System.out.println("Play completed.  Now we are recording!!!");
                if (playing) {
                    record();
                }
            });
            System.out.println("Finished creating soundCLip");
        } catch (IOException ex) {
            Log.e(ex);
            playing = false;
            return;
        }
        status.setText("");
        instructions.setText("Please wait");
        Log.p("soundClip.playAsync()");
        soundClip.playAsync().ready(m->{
            Log.p("soundClip.playAsync() ready "+m.getDuration());
            if (!playing) {
                Log.p("By the time sound clip started playing, we had already stopped");
                return;
            }
            status.setText("Playing");
            instructions.setText("Just listen to yourself.");
            final long startCounter = m.getDuration();
            final long startTime = System.currentTimeMillis();
            slider.setProgress(100);
            final Timer t = new Timer();
            t.schedule(new TimerTask() {
                public void run() {
                    long elapsed = System.currentTimeMillis() - startTime;
                    recordingCounter = startCounter - elapsed;
                    if (recordingCounter <= 0) {
                        System.out.println("Play Counter has reached zero.");
                        t.cancel();
                    } else {
                        CN.callSerially(()->{
                            if (!playing) {
                                t.cancel();
                                return;
                            }
                             int progress = (int)Math.round(recordingCounter * 100.0 / startCounter);
                            Log.p("progress="+progress);
                            slider.setProgress(progress);
                            Form f = slider.getComponentForm();
                            if (f != null) {
                                f.revalidateWithAnimationSafety();
                            }
                        });
                    }
                }
            }, 200, 200);
        }).except(ex->{
            Log.p("Failed to play sound clip");
            Log.e(ex);
            Log.p("Let's try to record again");
            record();
        });
        
    }
    }

}
