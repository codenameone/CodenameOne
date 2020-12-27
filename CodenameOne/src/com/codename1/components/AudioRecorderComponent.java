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
package com.codename1.components;

import com.codename1.io.Log;
import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.media.MediaRecorderBuilder;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.Sheet;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionSource;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.util.EventDispatcher;
import java.io.IOException;

/**
 * A component for recording Audio from the device microphone.
 * 
 * <p>Example usage</p>
 * 
 * <script src="https://gist.github.com/shannah/3e4d6448a6baf5684c736fd018f76f87.js"></script>
 * 
 * <p>This component enables a full recording workflow. When the component is first displayed, it provides a "Record"
 * button to begin the recording.</p>
 * <img src="https://www.codenameone.com/img/developer-guide/components-audiorecordercomponent1.png" alt="AudioRecorderComponent while recording" />
 * 
 * <p>While the recording is in progress, it provides a "Done" button and a "Pause" button.  The "Pause" button allows pausing and 
 * continuing the recording.  The "Done" button indicates the recording is done.</p>
 * 
 * <p>After the user presses "Done", a preview screen is shown that allows the user to listen to the recording.  Then can choose
 * to either accept the recording, cancel it, or try again.</p>
 * <img src="https://www.codenameone.com/img/developer-guide/components-audiorecordercomponent2.png" alt="AudioRecorderComponent accept/reject screen" />
 * 
 * @author Steve Hannah
 * @since 7.0
 */
public class AudioRecorderComponent extends Container implements ActionSource {
    private Media media;
    private Button record, pause, done;
    private Label recordingInProgress, recordingOff;
    private EventDispatcher actionListeners = new EventDispatcher();
    private RecorderState state;
    private double recordAlpha = 1.0;
    private long recordingLength;
    private long lastRecordingStartTime;
    private Label recordingTime;
    
    /**
     * Enum for tracking the recorder state.
     */
    public static enum RecorderState {
        /**
         * The recorder is initializing.
         */
        Initializing,
        
        /**
         * The recorder is currently recording.
         */
        Recording,
        
        /**
         * The recorder is currently paused.
         */
        Paused,
        
        /**
         * The recording is currently pending.  This recorder is in this state while the user is deciding whether to accept
         * the recording.
         */
        Pending,
        
        /**
         * The user chose to cancel the recording.
         */
        Canceled,
        
        /**
         * The user has accepted the recording.
         */
        Accepted,
        
        /**
         * The recorder is initialized.
         */
        Initialized,
        
        /**
         * The recorder is not initialized yet.
         */
        NotInitialized
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        getComponentForm().registerAnimated(this);
    }

    @Override
    protected void deinitialize() {
        getComponentForm().deregisterAnimated(this);
        super.deinitialize(); 
    }
    
    
    
    
    
    private int counter=0;

    @Override
    public boolean animate() {
        
        if (state == RecorderState.Recording) {
            recordAlpha = 0.6 + 0.4 * Math.sin(counter * Math.PI / 180);
            counter+=2;
            counter = counter % 360;
            if (recordingTime != null) {
                int milli = recordingLength();
                int sec = milli / 1000;
                int seconds = sec % 60;
                int minutes = sec / 60;

                String secStr = seconds < 10 ? "0" + seconds : "" + seconds;
                String minStr = minutes < 10 ? "0" + minutes : "" + minutes;

                String txt = minStr + ":" + secStr + "." + (milli%1000);
                
                recordingTime.setText(txt);
            }
            return true;
        } else {
            recordAlpha = 1.0;
        }
        return super.animate();
    }

    private int recordingLength() {
        if (lastRecordingStartTime > 0) {
            return (int)((System.currentTimeMillis() - lastRecordingStartTime) + recordingLength);
        } else {
            return (int)recordingLength;
        }
    }
    
    /**
     * Creates a new audio recorder for the settings specified by the given builder.
     * @param builder The settings for creating the media recorder.
     */
    public AudioRecorderComponent(final MediaRecorderBuilder builder) {
        super(new BorderLayout());
        CN.callSerially(new Runnable() {
            public void run() {
                try {
                    media = MediaManager.createMediaRecorder(builder);
                    setState(RecorderState.Initialized);
                    setState(RecorderState.Paused);
                } catch (IOException ex) {
                    Log.e(ex);
                    setState(RecorderState.NotInitialized);
                }
            }
            
        });
        
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                switch (state) {
                    case Accepted:
                    case Canceled:
                        break;
                    default:
                        evt.consume();
                }
                switch (state) {
                    case NotInitialized:
                        evt.consume();
                        removeAll();
                        add(BorderLayout.CENTER, new SpanLabel("Failed to initialize media recorder."));
                        revalidateLater();
                        break;
                        
                    case Initializing:
                        recordingLength = 0;
                        done.setEnabled(false);
                        recordingInProgress.setVisible(false);
                        evt.consume();
                        removeAll();
                        add(BorderLayout.CENTER, new SpanLabel("Preparing media.  Please wait..."));
                        revalidateLater();
                        break;
                        
                    case Initialized:
                        evt.consume();
                        removeAll();
                        add(BorderLayout.CENTER, buildUI());
                        revalidateLater();
                        break;
                        
                    case Accepted:
                        removeAll();
                        revalidateLater();
                        break;
                        
                    case Canceled:
                        removeAll();
                        revalidateLater();
                        break;
                        
                    case Paused:
                        //recordingTime.setVisible(false);
                        if (lastRecordingStartTime > 0) {
                            recordingLength += System.currentTimeMillis() - lastRecordingStartTime;
                            lastRecordingStartTime = 0;
                        }
                        recordingInProgress.setVisible(false);
                        recordingOff.setVisible(true);
                        record.setHidden(false);
                        record.setEnabled(true);
                        record.setVisible(true);
                        pause.setHidden(true);
                        pause.setVisible(false);
                        pause.setEnabled(false);
                        revalidateLater();
                        break;
                        
                    case Recording:
                        recordingTime.setVisible(true);
                        lastRecordingStartTime = System.currentTimeMillis();
                        recordingInProgress.setVisible(true);
                        recordingOff.setVisible(false);
                        done.setEnabled(true);
                        record.setHidden(true);
                        record.setEnabled(false);
                        record.setVisible(false);
                        pause.setHidden(false);
                        pause.setVisible(true);
                        pause.setEnabled(true);
                        revalidateLater();
                        break;
                }
            }
            
        });
        recordingInProgress = new Label("Recording") {
            @Override
            public void paint(Graphics g) {
                int opacity = g.getAlpha();
                double alpha = opacity * recordAlpha;
                g.setAlpha((int)Math.round(alpha));
                super.paint(g);
                g.setAlpha(opacity);
                
            }
            
        };
        $(recordingInProgress).selectAllStyles()
                .setFgColor(0xff0000);
        FontImage.setMaterialIcon(recordingInProgress, FontImage.MATERIAL_MIC);
        recordingOff = new Label("");
        $(recordingOff).selectAllStyles()
                .setFgColor(0x666666);
        FontImage.setMaterialIcon(recordingOff, FontImage.MATERIAL_MIC_OFF);
        recordingTime = new Label();
        
        record = new Button(FontImage.MATERIAL_FIBER_MANUAL_RECORD);
        
        record.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent t) {
                media.play();
                setState(RecorderState.Recording);
            }
            
        });
        
        pause = new Button(FontImage.MATERIAL_PAUSE);
        pause.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent t) {
                media.pause();
                setState(RecorderState.Paused);
            }
            
        });
        
        $(record).selectAllStyles()
                .setFgColor(0xff0000)
                
                .setMaterialIcon(FontImage.MATERIAL_FIBER_MANUAL_RECORD, 10)
                
                ;
        
        $(pause).selectAllStyles()
                .setFgColor(0x666666)
                .setMaterialIcon(FontImage.MATERIAL_PAUSE_CIRCLE_OUTLINE, 10);
        
        done = new Button("Done");
        done.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent t) {
                
                if (builder.isRedirectToAudioBuffer()) {
                    // We were just redirecting to the audio buffer so we don't have any previews to speak of
                    media.cleanup();
                    setState(RecorderState.Paused);
                    setState(RecorderState.Accepted);
                    return;
                }
                
                final boolean[] closeHandled = new boolean[1];
                media.pause();
                setState(RecorderState.Paused);
                setState(RecorderState.Pending);
                final Sheet processingSheet = new Sheet(Sheet.findContainingSheet(done), "Preview");
                processingSheet.getContentPane().setLayout(new BorderLayout());
                Container center = BorderLayout.center(new SpanLabel("Processing... please wait"));
                processingSheet.getContentPane().add(BorderLayout.CENTER, center);
                processingSheet.show();
                final Sheet sheet = new Sheet(Sheet.findContainingSheet(done), "Preview");
                
                Button cancel = new Button("Cancel");
                cancel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        closeHandled[0] = true;
                        sheet.back();
                        setState(RecorderState.Canceled);
                        
                    }
                });
                Button startOver = new Button("Start over");
                startOver.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        closeHandled[0] = true;
                        sheet.back();
                        setState(RecorderState.Initializing);
                        
                        try {
                            media = MediaManager.createMediaRecorder(builder);
                            setState(RecorderState.Initialized);
                            setState(RecorderState.Paused);
                        } catch (IOException ex) {
                            setState(RecorderState.NotInitialized);
                        }
                    }
                    
                });
                Button accept = new Button("Accept");
                accept.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        closeHandled[0] = true;
                        sheet.back();
                        setState(RecorderState.Accepted);
                    }
                    
                });
                sheet.getContentPane().setLayout(new BorderLayout());
                
                Container mpContainer = new Container(BoxLayout.y());
                sheet.getContentPane().add(BorderLayout.CENTER, BoxLayout.encloseY(mpContainer, accept, startOver, cancel));

                media.cleanup();
                try {
                    MediaPlayer mp = new MediaPlayer(MediaManager.createMedia(builder.getPath(), false));
                    mp.setOnTopMode(false);
                    
                    mpContainer.add(mp);
                } catch (IOException ex) {
                    mpContainer.add(new Label("No Audio Received"));
                    accept.setEnabled(false);
                    
                }
                sheet.addBackListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        if (!closeHandled[0]) {
                            closeHandled[0] = true;
                            setState(RecorderState.Initializing);
                            try {
                                media = MediaManager.createMediaRecorder(builder);
                                setState(RecorderState.Initialized);
                                setState(RecorderState.Paused);
                            } catch (IOException ex) {
                                setState(RecorderState.NotInitialized);
                            }
                        }
                    }
                    
                });
                sheet.addCloseListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        if (!closeHandled[0]) {
                            closeHandled[0] = true;
                            setState(RecorderState.Canceled);
                        }
                    }
                    
                });
                sheet.show();
                
                
            }
            
        });
        
        
        
        
    }
    
    /**
     * Gets the recording state.  Use this method to check whether the user accepted
     * the recording, or canceled the recording.
     * @return A RecorderState.
     */
    public RecorderState getState() {
        return state;
    }
    
    private Container buildUI() {
        Container out = new Container(new BorderLayout());
        done.remove();
        recordingOff.remove();
        recordingInProgress.remove();
        out.add(BorderLayout.NORTH, BorderLayout.centerEastWest(null, done, LayeredLayout.encloseIn(recordingOff, recordingInProgress)));
        record.remove();
        Container center = new Container(new LayeredLayout());
        center.add(record);
        pause.remove();
        center.add(pause);
        out.add(BorderLayout.CENTER, BorderLayout.centerAbsolute(center));
        
        recordingTime.remove();
        out.add(BorderLayout.SOUTH, FlowLayout.encloseCenter(recordingTime));
        
        return out;
    }
    
    private void setState(RecorderState state) {
        if (this.state != state) {
            System.out.println("State is now "+state);
            this.state = state;
            actionListeners.fireActionEvent(new ActionEvent(this));
        }
    }
    
    /**
     * Adds a listener to be notified when the state changes. Only transitions to the {@link RecorderState#Accepted}
     * and {@link RecorderState#Canceled} states result in an event.
     * 
     * @param l The listener
     */
    public void addActionListener(ActionListener l) {
        actionListeners.addListener(l);
    }
    
    /**
     * Removes an action listener.
     * @param l The listener.
     */
    public void removeActionListener(ActionListener l) {
        actionListeners.removeListener(l);
    }

    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(CN.convertToPixels(100), CN.convertToPixels(50));
    }
    
    
    
    
}
