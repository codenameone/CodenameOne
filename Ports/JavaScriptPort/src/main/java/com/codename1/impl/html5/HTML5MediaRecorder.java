/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5;

import com.codename1.impl.html5.HTML5Implementation.JSRunnable;
import static com.codename1.impl.html5.HTML5Implementation._log;
import com.codename1.impl.html5.HTML5Media.NotAllowedHandler;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.media.AbstractMedia;
import com.codename1.media.AudioBuffer;
import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.media.MediaRecorderBuilder;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import static com.codename1.ui.CN.invokeAndBlock;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Sheet;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.MessageEvent;
import com.codename1.ui.events.MessageEvent.PromptPromise;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.util.AsyncResource;
import com.codename1.util.AsyncResult;
import com.codename1.util.EasyThread;
import com.codename1.util.SuccessCallback;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.dom.Event;
import com.codename1.html5.js.typedarrays.Float32Array;

/**
 *
 * @author shannah
 */
public class HTML5MediaRecorder extends AbstractMedia {
    
    private CN1AudioRecorder peer;
    //private boolean pauseFlag;
    private float[] pcmBuffer;
    private StateInternal currentState = new StateInternal();
    //private Set<RecordRequest> pendingRecordRequests = new HashSet<RecordRequest>();
    private RecordRequest pendingRecordRequest;
    private PauseRequestInternal pendingPauseRequest;
    private final Object onCompleteLock = new Object();
    private boolean waitingForComplete = false;
    private AudioBuffer audioBuffer;
    private EasyThread processingThread;
    private Sheet playMediaSheet;
    private final EventDispatcher stateListeners = new EventDispatcher();
    
    private static class StateInternal {
        private boolean recording, paused;
        
        StateInternal(boolean recording, boolean paused) {
            this.recording = recording;
            this.paused = paused;
        }
        
        StateInternal() {
            paused = true;
        }
        
        StateInternal(StateInternal state) {
            this.recording = state.recording;
            this.paused = state.paused;
        }

        @Override
        public String toString() {
            return "State{recording:"+recording+", paused:"+paused+"}";
        }
        
        
    }

    
    private static class StateChangeEvent extends ActionEvent {
        private StateInternal oldState, newState;
        
        StateChangeEvent(HTML5MediaRecorder source, StateInternal oldState, StateInternal newState) {
            super(source);
            this.oldState = oldState;
            this.newState = newState;
        }
    }
    
    private void setState(StateInternal newState) {
        StateInternal oldState = new StateInternal(currentState);
        currentState = new StateInternal(newState);
        System.out.println("HTML5MediaRecorder setState("+newState+")");
        stateListeners.fireActionEvent(new StateChangeEvent(this, oldState, new StateInternal(newState)));
        if (newState.recording && !oldState.recording) {
            fireMediaStateChange(State.Playing);
        } else if (newState.paused && !oldState.paused) {
            fireMediaStateChange(State.Paused);
        }
    }
            
    
    @JSFunctor
    private interface StringCallback extends JSObject {
        public void callback(String arg);
    }
    
    public HTML5MediaRecorder(MediaRecorderBuilder builder) {
        if (builder.isRedirectToAudioBuffer()) {
            audioBuffer = MediaManager.getAudioBuffer(builder.getPath(), true, 256);
            pcmBuffer = new float[audioBuffer.getMaxSize()];
            final float[] fPcmBuffer = pcmBuffer;
            processingThread = EasyThread.start("AudioBufferProcessor");
            final EasyThread fProcessingThread = processingThread;
            final AudioBuffer fAudioBuffer = audioBuffer;
            peer = createAudioUnit(builder.getSamplingRate(), 16, builder.getAudioChannels(), new CN1AudioProcessor() {
                @Override
                public void onAudioProcess(final int sampleRate, final int numChannels, final Float32Array data) {
                    
                    new Thread(new Runnable() {
                        public void run() {
                            
                            fProcessingThread.run(new Runnable() {
                                public void run() {
                                    
                                    int len = data.getLength();
                                    int sampleBufferPos = 0;
                                    int audioBufferLen = fAudioBuffer.getMaxSize();
                                    for (int i= 0; i < len; i++) {
                                        fPcmBuffer[sampleBufferPos] = data.get(i);
                                        sampleBufferPos++;
                                        if (sampleBufferPos >= audioBufferLen) {
                                            fAudioBuffer.copyFrom(sampleRate, numChannels, fPcmBuffer, 0, sampleBufferPos);
                                            sampleBufferPos = 0;
                                        }
                                    }

                                    if (sampleBufferPos > 0) {
                                        fAudioBuffer.copyFrom(sampleRate, numChannels, fPcmBuffer, 0, sampleBufferPos);
                                        sampleBufferPos = 0;
                                    }
                                    

                                }
                            });
                        }
                            
                            
                        
                    }).start();
                    
                    
                    
                }
            }, new StringCallback() {
                @Override
                public void callback(final String arg) {
                    new Thread(new Runnable() {
                        public void run() {
                            if (!currentState.paused) {
                                
                                StateInternal newState = new StateInternal(currentState);
                                newState.paused = true;
                                newState.recording = false;
                                System.out.println("HTML5MediaRecorder setting state in onComplete callback");
                                setState(newState);
                            }
                            synchronized(onCompleteLock) {
                                waitingForComplete = false;
                                onCompleteLock.notifyAll();
                            }
                        }


                    }).start();
                }

                        },
            new StringCallback() {
                @Override
                public void callback(final String arg) {
                    new Thread(new Runnable() {
                        public void run() {
                            fireMediaError(new MediaException(MediaErrorType.LineUnavailable, arg));
                        }
                    }).start();
                    
                }
                
            },
            new OnRecord() {
                @Override
                public void recorderStarted(final int numChannels, final int sampleRate) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            
                            CN.callSerially(new Runnable() {
                                public void run() {
                                    if (!currentState.recording) {
                                        System.out.println("HTML5MediaRecorder setting state in onRecord callback");
                                        StateInternal newState = new StateInternal(currentState);
                                        newState.recording = true;
                                        newState.paused = false;
                                        setState(newState);
                                    }
                                   
                                }
                            });
                            
                           
                        }

                    }).start();
                }

            });
        } else {
            peer = createAudioRecorder(builder.getPath(),
                    new StringCallback() {
                        @Override
                        public void callback(final String arg) {
                            new Thread(new Runnable() {
                                public void run() {
                                    if (currentState.recording) {
                                        StateInternal newState = new StateInternal(currentState);
                                        newState.recording = false;
                                        newState.paused = true;
                                        setState(newState);
                                    }
                                    synchronized(onCompleteLock) {
                                        waitingForComplete = false;
                                        onCompleteLock.notifyAll();
                                    }
                                }

                            }).start();
                        }

                    },
                    new StringCallback() {
                        @Override
                        public void callback(final String arg) {
                            new Thread(new Runnable() {
                                public void run() {
                                    fireMediaError(new MediaException(MediaErrorType.LineUnavailable, arg));
                                }
                            }).start();
                            
                        }

                    },
                    new OnRecord() {
                        @Override
                        public void recorderStarted(int numChannels, int sampleRate) {
                            CN.callSerially(new Runnable() {
                                public void run() {
                                    if (!currentState.recording) {
                                        StateInternal newState = new StateInternal(currentState);
                                        newState.recording = true;
                                        newState.paused = false;
                                        setState(newState);
                                    }
                                }
                            });
                        }
                    }
                );
        }
    }
    
    
    @JSBody(params={"path", "onComplete", "onError", "onRecord"}, script="return new CN1AudioRecorder({onComplete:onComplete, savePath:path, onError:onError, onRecord:onRecord});")
    private native static CN1AudioRecorder createAudioRecorder(String path, StringCallback onComplete, StringCallback onError, OnRecord onRecord);

    @JSBody(params={"sampleRate", "sampleSize", "audioChannels", "onAudioProcess", "onComplete", "onError", "onRecord"}, script="return new CN1AudioUnit({sampleRate:sampleRate, sampleSize:sampleSize, audioChannels:audioChannels, onAudioProcess:onAudioProcess, onComplete:onComplete, onError:onError, onRecord:onRecord});")
    private native static CN1AudioRecorder createAudioUnit(int sampleRate, int sampleSize, int audioChannels, CN1AudioProcessor onAudioProcess, StringCallback onComplete, StringCallback onError, OnRecord onRecord);
    
    @JSBody(params={}, script="if (window.recordRequestComplete){window.recordRequestComplete();}")
    private native static void fireRecordRequestComplete();
    
    private class RecordRequest extends AsyncResource<Media> {
        private boolean disallowed;
        private boolean promptedByUser;
        private boolean canceled;

        @Override
        public void complete(Media value) {
            HTML5Implementation._log("Record request complete");
            fireRecordRequestComplete();
            if (pendingRecordRequest == this) {
                pendingRecordRequest = null;
            }
            super.complete(value);
        }

        @Override
        public void error(Throwable t) {
            if (pendingRecordRequest == this) {
                pendingRecordRequest = null;
            }
            fireRecordRequestComplete();
            HTML5Implementation._log("Record request complete with error ");
            super.error(t);
        }
        
    }
    
    private class PauseRequestInternal extends AsyncResource<Media> {

        @Override
        public void complete(Media value) {
            if (pendingPauseRequest == this) {
                pendingPauseRequest = null;
            }
            super.complete(value); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void error(Throwable t) {
            if (pendingPauseRequest == this) {
                pendingPauseRequest = null;
            }
            super.error(t); //To change body of generated methods, choose Tools | Templates.
        }
        
        
        
    }
    
   
    
    
    public RecordRequest playAsyncInternal() {
        return playAsync(new RecordRequest());
    }

    @Override
    public PlayRequest playAsync() {
        if (!CN.isEdt()) {
            Log.e(new IllegalArgumentException("WARNING: Calling MediaRecorder.playAsync() off the EDT."));
        }
        final PlayRequest out = new PlayRequest();
        playAsyncInternal().ready(new SuccessCallback<Media>() {
            @Override
            public void onSucess(Media t) {
                if (!out.isDone()) {
                    out.complete(HTML5MediaRecorder.this);
                }
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable t) {
                if (!out.isDone()) {
                    out.error(t);
                }
            }
        });
        return out;
    }
    
    private MessageEvent currPrompt;
    
    public RecordRequest playAsync(final RecordRequest out) {
        if (!CN.isEdt()) {
            CN.callSerially(new Runnable() {
                public void run() {
                    playAsync(out);
                }
            });
            return out;
        }
        System.out.println("HTML5MediaRecorder.playAsync "+currentState);
        if (out.isDone()) {
            // The request was already completed.
            // We don't need to proceed any further.
            return out;
        }
        
        if (pendingPauseRequest != null) {
            // There is a pending pause request
            // We need to attach to that pause request and play after the pause is complete
            System.out.println("HTML5MediaRecorder.playAsync - pendingPauseRequest.ready()");
            pendingPauseRequest.ready(new SuccessCallback<Media>() {
                @Override
                public void onSucess(Media t) {
                    System.out.println("HTML5MEdiaRecorder.playAsync - Pause resolved onSuccess.  Now what? "+out.isDone());
                    if (!out.isDone()) {
                        
                        playAsync(out);
                    }
                }
            }).except(new SuccessCallback<Throwable>() {
                @Override
                public void onSucess(Throwable t) {
                    System.out.println("HTML5MEdiaRecorder.playAsync - Pause resolved onError.  Now what? "+out.isDone());
                    if (!out.isDone()) {
                        playAsync(out);
                    }
                }
            });
            // As far as anyone that comes after is concerned, there
            // is no longer a pending pause request.  There is a pending
            // record request - *this* request.
            pendingPauseRequest = null;
            pendingRecordRequest = out;
            return out;
        }
        
        if (pendingRecordRequest != null && pendingRecordRequest != out) {
            // There is a pending record request.  We need to wait until that request 
            // is complete.
            System.out.println("HTML5MediaRecorder.playAsync - pendingRecordRequest.ready()");
            pendingRecordRequest.ready(new SuccessCallback<Media>() {
                @Override
                public void onSucess(Media t) {
                    if (out.isDone()) {
                        return;
                    }
                    out.complete(t);
                }
            }).except(new SuccessCallback<Throwable>() {
                @Override
                public void onSucess(Throwable t) {
                    if (out.isDone()) {
                        return;
                    }
                    out.error(t);
                }
            });
            return out;
        }
        
        // If we are here then there are no pending requests.
        // Mark ourself as the pending request.
        
        pendingRecordRequest = out;
        if (currentState.recording) {
            // We were already recording.
            // Do nothing and just complete the request.
            System.out.println("HTML5Media.playAsync - already recording");
            out.complete(this);
            return out;
        }
        
        // If we are here, then there are no pending requests AND we are not
        // currently recording.  We should be clear to record.
        
        // We install a change listener to listen for errors or a change of state
        // to recording.
        // If either of these things happen, then we know that the "play" took effect.
        class StateChangeListener implements ActionListener {
            ActionListener<MediaErrorEvent> onError;
            @Override
            public void actionPerformed(ActionEvent t) {
                StateChangeEvent evt = (StateChangeEvent)t;
                
                if (out.isDone()) {
                    stateListeners.removeListener(this);
                    if (onError != null) {
                        removeMediaErrorListener(onError);
                    }
                    return;
                }
                
                if (evt.newState.recording) {
                    // The state of this media has changed to recording
                    // We can remove the state listener we added
                    // (and error listener), and complete the promise
                    stateListeners.removeListener(this);
                    if (onError != null) {
                        removeMediaErrorListener(onError);
                    }
                    System.out.println("HTML5MediaRecorder record seemed to work.  Current state "+currentState);
                    out.complete(HTML5MediaRecorder.this);
                    return;
                }
                
                
            }
            
        };
        final StateChangeListener onStateChange = new StateChangeListener();
        
        final ActionListener<MediaErrorEvent> onError = new ActionListener<MediaErrorEvent>() {
            @Override
            public void actionPerformed(MediaErrorEvent t) {
                stateListeners.removeListener(onStateChange);
                removeMediaErrorListener(this);
                if (!out.isDone()) {
                    System.out.println("HTML5MediaRecorder.playAsync - media error: "+t.getMediaException().getMessage());
                    out.error(t.getMediaException());
                }
            }
            
        };
        onStateChange.onError = onError;
        stateListeners.addListener(onStateChange);
        addMediaErrorListener(onError);
        
        if (!out.disallowed) {
            System.out.println("HTML5MediaRecorder playAsync !out.disallowed");
            // We haven't been denied access yet, so we should at least try.
            peer.record(new NotAllowedHandler() {
                @Override
                public void onNotAllowed() {
                    // This callback is on the JS main thread.  We need to wrap
                    // it in a Java thread then run it on the EDT
                    HTML5Implementation.callSerially(new Runnable() {
                        public void run() {
                            stateListeners.removeListener(onStateChange);
                            removeMediaErrorListener(onError);
                            if (out.isDone()) {
                                return;
                            }
                            out.disallowed = true;
                            playAsync(out);
                            
                        }
                    });
                }
                
            });
            return out;
        } 
        
        // If we are here, then we have already attempted recording once and 
        // been denied by the browser.  Probably access was denied because 
        // we weren't running in direct response to user interaction.
        // We have some options remaining.  The JS port installs "back-side" hooks
        // whenever the pointer is pressed, which are basically setTimeout() calls
        // which will run a callback delayed by a few hundred milliseconds.  
        // First we check if there are any back-side hooks available to latch onto.
        HTML5Implementation impl = HTML5Implementation.getInstance();
        if (impl.isBacksideHookAvailable()) {
            // As luck would have it, there is a back-side hook available.
            // A back-side hook is installed every time the user presses or releases the 
            // pointer, and they hang around for long enough to react to the press
            // inside a back-side hook.
            System.out.println("HTML5MediaRecorder playAsync impl.backsidehookAvailable");
            impl.addBacksideHook(new JSRunnable() {
                @Override
                public void run() {
                    HTML5Implementation._log("Running backside hook");
                    
                    if (out.isDone()) {
                        HTML5Implementation._log("Record request is already done.");
                        // The request was already completed somehow.
                        // Remember to remove the state and error listeners
                        // and back away quietly.
                        new Thread(new Runnable() {
                            public void run() {
                                stateListeners.removeListener(onStateChange);
                                removeMediaErrorListener(onError);
                            }
                        }).start();
                        return;
                    }
                    
                    // Now that we are on the main thread we can try to issue
                    // a record again and it *should* work.
                    peer.record(new NotAllowedHandler() {
                        @Override
                        public void onNotAllowed() {
                            // OKay  we got denied AGAIN!!!
                            // Let's just remove our state listeners
                            // return an error and throw our hands up in the air.
                            // Alas, we tried :(
                            HTML5Implementation.callSerially(new Runnable() {
                                public void run() {
                                    stateListeners.removeListener(onStateChange);
                                    removeMediaErrorListener(onError);
                                    if (out.isDone()) {
                                        return;
                                    }
                                    MediaException ex = new MediaException(MediaErrorType.Aborted, "Media recording disallowed by browser permissions.");
                                    out.error(ex);
                                    fireMediaError(ex);
                                }
                            
                            });
                        }
                    });
                    
                    // NOTE: We don't need to add any more code here to cover the "Success" case
                    // because the state listener should be fired upon a successful record.
                }
                
            });
            return out;
        } else {
            System.out.println("No back-side hook available");
        }
        
        // For the remainder of this, we really need to be on the EDT.
        if (!CN.isEdt()) {
            HTML5Implementation.callSerially(new Runnable() {
                public void run() {
                    playAsync(out);
                }
            });
            return out;
        }
        
        //If we are here then there were no back-side hooks available to latch onto.
        // We will try to manufacture a new back-side hook by compelling the user to 
        // press somewhere on the screen.  We do this by prompting the user.
        if (out.promptedByUser) {
            // We've already prompted the user, 
            // and there were no backside cache options last time
            // so let's just report an error
            System.out.println("HTML5MediaRecorder playAsync out.promptedByUser");
            stateListeners.removeListener(onStateChange);
            removeMediaErrorListener(onError);
            
            out.error(new MediaException(MediaErrorType.Aborted, "Recording disallowed by browser."));
            return out;
        }
        // Let the outside webpage know that we are prompting the user for interaction
        // so that it can display the iframe containing the app if deployed headlessly.
        if (currPrompt != null && !currPrompt.getPromptPromise().isDone()) {
            currPrompt.getPromptPromise().onResult(new AsyncResult<Boolean>() {
                @Override
                public void onReady(final Boolean res, final Throwable err) {
                    
                    // NOTE: We don't need to dispatch prompt complete events from this callback
                    // becuse they would have been dispatched in the "currPrompt" that we are piggy-backing
                    // onto.
                    stateListeners.removeListener(onStateChange);
                    removeMediaErrorListener(onError);
                    if (out.isDone()) {
                        
                        //Window.current().dispatchEvent(createJavascriptPromptCompleteEvent(false));
                        return;
                    }
                    if (err == null && res) {
                        out.promptedByUser = true;
                        //Window.current().dispatchEvent(createJavascriptPromptCompleteEvent(true));
                        playAsync(out);
                    } else {
                        //Window.current().dispatchEvent(createJavascriptPromptCompleteEvent(false));
                        out.error(new MediaException(MediaErrorType.Aborted, "Record canceled by the user"));
                    }
                }
                       
            });
            return out;
        }
        
        // Let the outside webpage know that we are prompting the user for interaction
        // so that it can display the iframe containing the app if deployed headlessly.
        Window.current().dispatchEvent(createJavascriptPromptEvent("AUDIO_RECORDER_READY"));
        // Give the app developer a chance to create his own dialog
        PromptPromise result = new PromptPromise();
        final MessageEvent promptEvent = new MessageEvent(result, "Audio Recorder Ready", 427);
        currPrompt = promptEvent;
        result.onResult(new AsyncResult<Boolean>() {
            @Override
            public void onReady(final Boolean res, final Throwable err) {
                if (!CN.isEdt()) {
                    CN.callSerially(new Runnable() {
                        public void run() {
                            onReady(res, err);
                    
                        }
                    });
                    //onReady(res, err);
                    return;
                }
                if (currPrompt == promptEvent) {
                    currPrompt = null;
                }
                stateListeners.removeListener(onStateChange);
                removeMediaErrorListener(onError);
                if (out.isDone()) {
                    Window.current().dispatchEvent(createJavascriptPromptCompleteEvent(false));
                    return;
                }
                if (err == null && res) {
                    out.promptedByUser = true;
                    Window.current().dispatchEvent(createJavascriptPromptCompleteEvent(true));
                    playAsync(out);
                } else {
                    Window.current().dispatchEvent(createJavascriptPromptCompleteEvent(false));
                    out.error(new MediaException(MediaErrorType.Aborted, "Record canceled by the user"));
                }
            }
        });
        
        
        
        Display.getInstance().dispatchMessage(promptEvent);
        if (promptEvent.isConsumed()) {
            // If the app consumed this message event, that means it is handling the prompt
            // we can just return
            System.out.println("Prompt event was consumed");
            return out;
        }
        
        currPrompt = null;
        
        /*
        if (!Window.confirm("This application would like to access your microphone")) {
            stateListeners.removeListener(onStateChange);
            removeMediaErrorListener(onError);
            out.error(new MediaException(MediaErrorType.Aborted, "Record canceled by the user"));
            Window.current().dispatchEvent(createJavascriptPromptCompleteEvent(false));
            return out;
        }
        stateListeners.removeListener(onStateChange);
        removeMediaErrorListener(onError);
        out.promptedByUser = true;
        Window.current().dispatchEvent(createJavascriptPromptCompleteEvent(true));
        playAsync(out);
        */
        //if (true) return out;
        
        System.out.println("About to show sheet");
        if (playMediaSheet != null) {
            playMediaSheet.back();
            playMediaSheet = null;
        }
        Sheet currSheet = Sheet.getCurrentSheet();

        final Sheet sheet = new Sheet(currSheet, "Audio Recorder Ready");
        final boolean[] recordPressed = new boolean[1];
        Button playButton = new Button("Start Recording");
        playButton.setMaterialIcon(FontImage.MATERIAL_RECORD_VOICE_OVER);
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (playMediaSheet == sheet) {
                    playMediaSheet = null;
                }
                stateListeners.removeListener(onStateChange);
                removeMediaErrorListener(onError);
                recordPressed[0] = true;
                if (out.isDone()) {
                    sheet.back();
                    return;
                }
                out.promptedByUser = true;
                playAsync(out);

                
                Window.current().dispatchEvent(createJavascriptPromptCompleteEvent(true));
                sheet.back();
            }

        });
        sheet.addCloseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent t) {
                if (playMediaSheet == sheet) {
                    playMediaSheet = null;
                }
                stateListeners.removeListener(onStateChange);
                removeMediaErrorListener(onError);
                if (out.isDone()) {
                    
                    if (!recordPressed[0]) {
                        sheet.back();
                        Window.current().dispatchEvent(createJavascriptPromptCompleteEvent(false));
                    }
                    return;
                }
                if (!recordPressed[0]) {
                    out.error(new MediaException(MediaErrorType.Aborted, "Record canceled by the user"));
                    Window.current().dispatchEvent(createJavascriptPromptCompleteEvent(false));
                }
            }

        });
        sheet.getContentPane().setLayout(BoxLayout.y());
        sheet.getContentPane().add(FlowLayout.encloseCenter(playButton));
        playMediaSheet = sheet;
        
        sheet.show();
        return out;
    }

    @JSBody(params={"description"}, script="return new CustomEvent('cn1userprompt', {detail: description})")
    private native static Event createJavascriptPromptEvent(String description);
    
    @JSBody(params={"response"}, script="return new CustomEvent('cn1userpromptresponse', {detail: response})")
    private native static Event createJavascriptPromptCompleteEvent(boolean response);
    
    
    @Override
    protected void pauseImpl() {
        throw new RuntimeException("Shouldn't need to implements pauseImpl because we override pauseAsync");
    }

    @Override
    protected void playImpl() {
        throw new RuntimeException("Shouldn't need to implement playImpl because we overrid playAsync");
    }
    
    
            
    
    

    @Override
    public PauseRequest pauseAsync() {
        final PauseRequest out = new PauseRequest();
        if (!CN.isEdt()) {
            Log.e(new IllegalArgumentException("WARNING: Calling MediaRecorder.pauseAsync() off the EDT"));
        }
        pauseAsyncInternal().ready(new SuccessCallback<Media>() {
            @Override
            public void onSucess(Media t) {
                if (!out.isDone()) {
                    out.complete(HTML5MediaRecorder.this);
                }
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable t) {
                if (!out.isDone()) {
                    out.error(t);
                }
            }
        });
        return out;
    }
    
    
    
    private PauseRequestInternal pauseAsyncInternal() {
        return pauseAsync(new PauseRequestInternal());
    }
    
    private PauseRequestInternal pauseAsync(final PauseRequestInternal out) {
        if (!CN.isEdt()) {
            CN.callSerially(new Runnable() {
                public void run() {
                    pauseAsync(out);
                }
            });
            return out;
        }
        if (out.isDone()) {
            return out;
        }
        if (pendingRecordRequest != null) {
            // There is a pending record request.  We'll wait for that request
            // to complete, then we'll immediately issue a pause.
            pendingRecordRequest.ready(new SuccessCallback<Media>() {
                @Override
                public void onSucess(Media t) {
                    if (!out.isDone()) {
                        pauseAsync(out);
                    }
                }
            }).except(new SuccessCallback<Throwable>() {
                @Override
                public void onSucess(Throwable t) {
                    if (!out.isDone()) {
                        pauseAsync(out);
                    }
                }
            });
            
            // As far as we're concerned, the current pending request is now *this*
            // request.  Remove the pending record request, and replace it with this one.
            pendingRecordRequest = null;
            pendingPauseRequest = out;
            return out;
        }
        
        // If we are here, then there is no pending record request.
        
        if (pendingPauseRequest != null && pendingPauseRequest != out) {
            // There is another pending pause request.
            pendingPauseRequest.ready(new SuccessCallback<Media>() {
                @Override
                public void onSucess(Media t) {
                    if (!out.isDone()) {
                        out.complete(t);
                    }
                }
            }).except(new SuccessCallback<Throwable>() {
                @Override
                public void onSucess(Throwable t) {
                    if (!out.isDone()) {
                        out.error(t);
                    }
                }
            });
            return out;
        }
        
        if (currentState.paused) {
            out.complete(this);
            return out;
        }
        
        
        // If we are here, then there is no existing pending pause request or record request
        // Set ourself as the pending pause request.
        pendingPauseRequest = out;
        if (peer != null) {
            // Issue the pause() call to the peer.
            peer.pause();
        }
        Timer t = new Timer();
        
        t.schedule(new TimerTask(){
            public void run() {
                CN.callSerially(new Runnable() {
                    public void run() {
                        StateInternal newState = new StateInternal(currentState);
                        newState.paused = true;
                        newState.recording = false;
                        setState(newState);
                        out.complete(HTML5MediaRecorder.this);
                    }
                    
                });
                
            }
        }, getPauseDelay());
        
        return out;
        
    }

    @JSBody(params={}, script="return window.cn1HTML5MediaRecorderPauseDelay || 1")
    private static native int getPauseDelay();
    
    @Override
    public void prepare() {
        
    }

    @Override
    public void cleanup() {
        
        if (peer == null) {
            return;
        }
        pause();
        
        peer.stop();
        waitingForComplete = true;
        invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                while (waitingForComplete) {
                    synchronized(onCompleteLock) {
                        Util.wait(onCompleteLock);
                    }
                }
            }
            
        });
        if (processingThread != null) {
            processingThread.run(new Runnable() {
                public void run() {
                    if (processingThread != null) {
                        processingThread.kill();
                        processingThread = null;
                    }
                }
            });
        }
        peer = null;
       
    }
    
     protected void finalize() {
        if(peer != null) {
            cleanup();
        }
    }

    @Override
    public int getTime() {
        return 0;
    }

    @Override
    public void setTime(int i) {
        
    }

    @Override
    public int getDuration() {
        return 0;
    }

    @Override
    public void setVolume(int i) {
        
    }

    @Override
    public int getVolume() {
        return 0;
    }

    @Override
    public boolean isPlaying() {
        
        return currentState.recording;
    }

    @Override
    public Component getVideoComponent() {
        return null;
    }

    @Override
    public boolean isVideo() {
        return false;
    }

    @Override
    public boolean isFullScreen() {
        return false;
    }

    @Override
    public void setFullScreen(boolean bln) {
        
    }

    @Override
    public void setNativePlayerMode(boolean bln) {
        
    }

    @Override
    public boolean isNativePlayerMode() {
        return false;
    }

    @Override
    public void setVariable(String string, Object o) {
        
    }

    @Override
    public Object getVariable(String string) {
        return null;
    }
    
    @JSFunctor
    public static interface OnRecord extends JSObject {
        public void recorderStarted(int numChannels, int sampleRate);
    }
    
    public interface CN1AudioRecorder extends JSObject {
        public void record(HTML5Media.NotAllowedHandler notAllowedCallback);
        public void pause();
        public void stop();
        public void resume();
        public boolean isRecording();
    }
    
  
    
    @JSFunctor
    public interface CN1AudioProcessor extends JSObject {
        public void onAudioProcess(int sampleRate, int numChannels, com.codename1.html5.js.typedarrays.Float32Array data);
    }
    
    
}
