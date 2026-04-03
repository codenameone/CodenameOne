/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5;

import static com.codename1.impl.html5.HTML5Implementation._log;
import com.codename1.impl.html5.JSOImplementations.HTMLMediaElement;
import com.codename1.impl.html5.JSOImplementations.HTMLVideoElement;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.media.AbstractMedia;
import com.codename1.media.AsyncMedia;
import com.codename1.media.Media;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
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
import com.codename1.util.SuccessCallback;
import java.util.ArrayList;
import java.util.List;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.dom.Event;
import com.codename1.html5.js.dom.EventListener;
import com.codename1.html5.js.dom.HTMLElement;

/**
 *
 * @author shannah
 */
public class HTML5Media extends AbstractMedia {
    public static boolean microphoneActive;
    private HTMLMediaElement el;
    private final boolean isVideo;
   
    private MediaComponent component;
    private List<Runnable> completionHandlers;
    private StateInternal currentState = new StateInternal();
    private boolean pausePending;
    private int pendingTime=-1;

    private Sheet playMediaSheet;
    private final EventDispatcher stateListeners = new EventDispatcher();

    private PlayRequestInternal pendingPlayRequest;
    private PauseRequestInternal pendingPauseRequest;
    
    @JSBody(params={"el"}, script="if (window.cn1OnCreateMedia) window.cn1OnCreateMedia(el);")
    private static native void onCreateMedia(HTMLMediaElement el);
    
    public HTML5Media(HTMLMediaElement el, boolean isVideo){
        onCreateMedia(el);
        this.el = el;
        this.el.setVolume(1.0);
        this.el.setMuted(false);
        this.el.setControls(true); // Default is to show controls
            // use setHideNativeControls to hide them.
        this.isVideo = isVideo;
        if (isVideo) {
            el.setAttribute("playsinline", "");
        }
        final EventListener onPlay = new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                final StateInternal newState = new StateInternal(currentState);
                newState.playing = true;
                newState.paused = false;
                new Thread(new Runnable(){
                    public void run() {
                        setState(newState);
                    }
                }).start();
                
            }
              
        };
        final EventListener onPause = new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                final StateInternal newState = new StateInternal(currentState);
                newState.playing = false;
                newState.paused = true;
                pausePending = false;
                new Thread(new Runnable(){
                    public void run() {
                        setState(newState);
                    }
                }).start();
                
            }
            
        };
        final EventListener onCanPlay = new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                final boolean seeking = (pendingTime > 0);
                if (seeking) {
                    HTML5Media.this.el.setCurrentTime(pendingTime/1000.0);
                    pendingTime = 0;
                }
                final StateInternal newState = new StateInternal(currentState);
                newState.canPlay = true;
                new Thread(new Runnable(){
                    public void run() {
                        if (seeking) {
                            waitWhileSeeking(5000);
                        }
                        setState(newState);
                    }
                }).start();
            }
            
        };
        final EventListener onError = new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                if (HTML5Media.this.el == null) {
                    return;
                }
                
                final String errorMessage = getErrorMessage(HTML5Media.this.el);
                final int code = getErrorCode(HTML5Media.this.el);
                new Thread(new Runnable(){
                    public void run() {
                        fireMediaError(createMediaException(errorMessage, code));
                    }
                }).start();
                
            }
            
        };
        final EventListener onEnd = new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                if (!currentState.paused) {
                    final StateInternal newState = new StateInternal(currentState);
                    newState.playing = false;
                    newState.paused = true;
                    new Thread(new Runnable(){
                        public void run() {
                            setState(newState);
                        }
                    }).start();
                }
                
            }
            
        };
        
        initState();
        this.el.addEventListener("play", onPlay);
        this.el.addEventListener("pause", onPause);
        this.el.addEventListener("canplay", onCanPlay);
        this.el.addEventListener("error", onError);
        this.el.addEventListener("ended", onEnd);
        
        HTML5Implementation.getInstance().mediaPool().addCleanupListener(new HTML5MediaPool.CleanupListener(el) {
            @Override
            public void run(HTMLElement el) {
                el.removeEventListener("play", onPlay);
                el.removeEventListener("pause", onPause);
                el.removeEventListener("canplay", onCanPlay);
                el.removeEventListener("error", onError);
                el.removeEventListener("ended", onEnd);
            }
        });
    }
    
    private static MediaErrorType getMediaErrorType(int code) {
        switch (code) {
            case 1:
                return MediaErrorType.Aborted;
            case 2:
                return MediaErrorType.Network;
            case 3:
                return MediaErrorType.Decode;
            case 4:
                return MediaErrorType.SrcNotSupported;
            default:
                return MediaErrorType.Unknown;
                
        }
    }
    
    private static MediaException createMediaException(String message, int code) {
        return new MediaException(getMediaErrorType(code), message);
    }
    
    private void initState() {
        if (this.el == null) {
            return;
        }
        if (this.el.getReadyState() >= 3) {
            currentState.canPlay = true;
        }
        if (currentState.canPlay && el.getCurrentTime() > 0 && !el.isPaused() && !el.isEnded()) {
            currentState.playing = true;
            currentState.paused = false;
        }
    }
    
    private void fireStateChange(final StateInternal oldState, final StateInternal newState) {
        new Thread(new Runnable() {
            public void run() {
                stateListeners.fireActionEvent(new StateChangeEventInternal(HTML5Media.this, oldState, newState));
            }
        }).start();
    }
    
   
    
    private static class StateInternal {
        private boolean playing, paused, canPlay;
        
        StateInternal() {
            this(false, false, true);
        }
        
        StateInternal(boolean canPlay, boolean playing, boolean paused) {
            this.playing = playing;
            this.paused = paused;
        }
        
        StateInternal(StateInternal state) {
            this.canPlay = state.canPlay;
            this.playing = state.playing;
            this.paused = state.paused;
        }
        
        
    }
    
    private void setState(StateInternal state) {
        StateInternal oldState = new StateInternal(currentState);
        currentState = state;
        System.out.println("Setting state to "+state);
        fireStateChange(oldState, new StateInternal(state));
        if (state.playing && !oldState.playing) {
            fireMediaStateChange(AsyncMedia.State.Playing);
        } else if (state.paused && !oldState.paused) {
            fireMediaStateChange(AsyncMedia.State.Paused);
        }
        
    }
    
    private static class StateChangeEventInternal extends ActionEvent {
        private StateInternal oldState;
        private StateInternal newState;
        
        StateChangeEventInternal(Object source, StateInternal oldState, StateInternal newState) {
            super(source);
            this.oldState = oldState;
            this.newState = newState;
        }
        
    }
    

    public HTMLMediaElement getMediaElement() {
        return el;
    }
    
    public void addCompletionHandler(Runnable r) {
        if (this.el == null) {
            return;
        }
        if (completionHandlers == null) {
            completionHandlers = new ArrayList<Runnable>();
            final EventListener endedListener = new EventListener(){

                @Override
                public void handleEvent(Event evt) {
                    if (!currentState.paused) {
                        final StateInternal newState = new StateInternal(currentState);
                        newState.playing = false;
                        newState.paused = true;
                        new Thread(new Runnable(){
                            public void run() {
                                setState(newState);
                            }
                        }).start();
                    }
                    new Thread(){
                        public void run(){
                            Display.getInstance().callSerially(new Runnable() {
                                public void run() {
                                    List<Runnable> toRun = new ArrayList<Runnable>(completionHandlers);
                                    for (Runnable handler : toRun) {
                                        handler.run();
                                    }
                                }
                            });
                        }
                    }.start();
                }
                
            };
            el.addEventListener("ended", endedListener);
            HTML5Implementation.getInstance().mediaPool().addCleanupListener(new HTML5MediaPool.CleanupListener(el) {
                @Override
                public void run(HTMLElement theEl) {
                    theEl.removeEventListener("ended", endedListener);
                }
            });
        }
        completionHandlers.add(r);
    }
    
    public void removeCompletionHandler(Runnable r) {
        if (completionHandlers != null) {
            completionHandlers.remove(r);
        }
    }
    
    @Override
    public Component getVideoComponent() {
        if (component == null) {
            component = new MediaComponent();
        }
        return component;
    }

    private static boolean playMethodReturnsPromise;
    private static boolean playMethodReturnsPromiseChecked;
    
    @JSBody(params={}, script="try { var promise = document.createElement('video').play(); return promise !== undefined;} catch (e) {return false;}")
    private native static boolean _playMethodReturnsPromise();
    private static boolean playMethodReturnsPromise() {
        if (!playMethodReturnsPromiseChecked) {
            playMethodReturnsPromiseChecked = true;
            playMethodReturnsPromise = _playMethodReturnsPromise();
            //HTML5Implementation._log("Play method returns promise? "+playMethodReturnsPromise);
        }
        return playMethodReturnsPromise;
    }
    
    /**
     * Callback used for {@link #playCatch(com.codename1.impl.html5.JSOImplementations.HTMLMediaElement, com.codename1.impl.html5.HTML5Media.NotAllowedHandler) }
     * to handle the case where media playback was not allowed.
     */
    @JSFunctor
    static interface NotAllowedHandler extends JSObject {
        public void onNotAllowed();
    }
    
    @JSFunctor 
    static interface FailedToPlayHandler extends JSObject {
        public void failedToPlay(String msg);
    }
    
    
    @JSBody(params={"el", "onNotAllowed", "failedToPlay"}, script="try {\n"
            + "var promise = window.cn1Play ? window.cn1Play(el) : el.play(); \n"
            + "if (promise !== undefined) {\n"
            + "  promise.then(function(){\n"
            + "    el.setAttribute('data-cn1-unlocked', 'true');\n"
            + "    console.log('HTML5Media#playCatch: Audio playback started.  id='+el.getAttribute('cn1-audio-id'));\n"
            + "  });\n"
            + "}\n"
            + "promise.catch(function(err){\n"
            + " if (err.name=='NotAllowedError'){\n"
            + "     onNotAllowed();\n"
            + " } else {\n"
            + "     console.log('Failed to play media with id='+el.getAttribute('cn1-audio-id'), err, el);\n"
            + "     if (failedToPlay) {\n"
            + "         failedToPlay(''+err);\n"
            + "     }\n"
            + " }\n"
            + "});\n"
            + "} catch (ex){\n"
            + " console.log(ex);\n"
            + " if (failedToPlay) {\n"
            + "     failedToPlay(''+err);\n"
            + " }\n"
            + "}")
    private static native void playCatch(HTMLMediaElement el, NotAllowedHandler onNotAllowed, FailedToPlayHandler failedToPlay);
    
    /**
     * Plays media but catches any exceptions.  Only used when the browser doesn't support 
     * media.play() returning promises.    See compatibility chart for returning promises
     * at https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement/play
     * @param el 
     */
    @JSBody(params={"el"}, script="try {el.play()} catch (ex){console.log(ex);}")
    private static native void playSafe(HTMLMediaElement el);
    
   
    @JSBody(params={"el"}, script="if (!el) return ''; if (!el.error) return ''; try {return el.error.message;} catch (e) { return '';}")
    private static native String getErrorMessage(HTMLMediaElement el);
    
    @JSBody(params={"el"}, script="if (!el) return 0; if (!el.error) return 0; try {return el.error.code;} catch (e) { return 0;}")
    private static native int getErrorCode(HTMLMediaElement el);
    
    public PlayRequestInternal playAsyncInternal() {
        return playAsync(new PlayRequestInternal());
    }

    
    private class PlayRequestInternal extends AsyncResource<Media> {
        private boolean initiatedByUserPrompt;
        private boolean disallowed;

        @Override
        public void complete(Media value) {
            if (pendingPlayRequest == this) {
                pendingPlayRequest = null;
            }
            super.complete(value); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void error(Throwable t) {
            if (pendingPlayRequest == this) {
                pendingPlayRequest = null;
            }
            super.error(t); 
        }
        
        
        
        
    }
    
    /**
     * Loads the media then plays it.  This is called by playAsync()
     * if it detects that the media isn't loaded yet.
     * @param out
     * @return 
     */
    private PlayRequestInternal loadThenPlay(final PlayRequestInternal out) {
         // We're not in playable state yet.
        // Let's trigger a load on this media, and 
        // wait for the state to change so that we can play
        // Then we'll try to play again.

        class StateChangeListener implements ActionListener {
            ActionListener<MediaErrorEvent> onError;
            @Override
            public void actionPerformed(ActionEvent t) {
                if (out.isDone()) {
                    stateListeners.removeListener(this);
                    if (onError != null) {
                        removeMediaErrorListener(onError);
                    }
                    return;
                }
                StateChangeEventInternal evt = (StateChangeEventInternal)t;

                

                if (evt.newState.canPlay) {
                    stateListeners.removeListener(this);
                    if (onError != null) {
                        removeMediaErrorListener(onError);
                    }
                    playAsync(out);
                    return;
                }
            }

        };
        final StateChangeListener onStateChange = new StateChangeListener();
        
        ActionListener<MediaErrorEvent> onError = new ActionListener<MediaErrorEvent>() {
            @Override
            public void actionPerformed(MediaErrorEvent t) {
                removeMediaErrorListener(this);
                stateListeners.removeListener(onStateChange);
                if (out.isDone()) {
                    return;
                }
                
                out.error(t.getMediaException());
                
            }
            
        };
        onStateChange.onError = onError;
        stateListeners.addListener(onStateChange);
        addMediaErrorListener(onError);
        loadMedia(el);
        return out;
    }

    @Override
    public String toString() {
        if (el == null) {
            return "HTML5Media(null)";
        }
        String src = el.getSrc();
        return "HTML5Media("+src+")";
    }
    
    
    
    private PlayRequestInternal playAsyncForBrowsersThatReturnPromise(final PlayRequestInternal out) {
        // This browser will return a promise from the media play() method
        // so we can detect if play was disallowed.
        if (out.isDone()) {
            return out;
        }

        class StateChangeListener implements ActionListener {
            ActionListener<MediaErrorEvent> onError;
            @Override
            public void actionPerformed(ActionEvent t) {
                if (out.isDone()) {
                    stateListeners.removeListener(this);
                    if (onError != null) {
                        removeMediaErrorListener(onError);
                    }
                    return;
                }
                StateChangeEventInternal evt = (StateChangeEventInternal)t;
                if (evt.newState.playing) {
                    stateListeners.removeListener(this);
                    if (onError != null) {
                        removeMediaErrorListener(onError);
                    }
                    out.complete(HTML5Media.this);
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
                if (out.isDone()) {
                    return;
                }
                out.error(t.getMediaException());
            }
            
        };
        onStateChange.onError = onError;
        stateListeners.addListener(onStateChange);
        addMediaErrorListener(onError);
        if (!out.disallowed) {
            // This play request hasn't been disallowed yet
            // So we'll just go ahead and try to play the media.
            // If the browser blocks playback, as most modern browsers
            // do if playing media without a user interaction
            // then it will give us a not allowed error.
            // In that case we'll respond by prompting the user to play 
            // the media.
            final boolean wasUnlocked = isUnlocked(el);
            _log("HTML5Media#playAsyncForBrowsersThatReturnPromise: Attempt to play media id="+getMediaID(el)+", unlocked="+wasUnlocked+", disallowed=false");
            playCatch(el, new NotAllowedHandler() {
                @Override
                public void onNotAllowed() {
                    new Thread(new Runnable() {
                        public void run() {
                            _log("HTML5Media#playAsyncForBrowsersThatReturnPromise: Play not allowed. id="+getMediaID(el)+", unlocked="+wasUnlocked+", disallows=false");
                            stateListeners.removeListener(onStateChange);
                            removeMediaErrorListener(onError);
                            
                            if (out.isDone()) {
                                return;
                            }
                            if (out.initiatedByUserPrompt) {
                                // This was already initiated by a user prompt
                                // so the failure must have been for a reason that
                                // we can't remedy with a user prompt.
                                // In this case, just return the error
                                out.error(new MediaException(MediaErrorType.Aborted, "Play disallowed by the user or browser"));
                            } else {
                                // This wasn't initiated by a user prompt
                                // so we can give that a try.
                                out.disallowed = true;
                                CN.callSerially(new Runnable() {
                                    public void run() {
                                        playAsync(out);
                                    }
                                });
                                
                            }

                        }
                    }).start();
                }
            }, new FailedToPlayHandler() {
                @Override
                public void failedToPlay(final String msg) {
                    new Thread(new Runnable() {
                        public void run() {
                            _log("HTML5Media#playAsyncForBrowsersThatReturnPromise: Failed to play. id="+getMediaID(el)+", unlocked="+wasUnlocked+", disallows=false");
                            stateListeners.removeListener(onStateChange);
                            removeMediaErrorListener(onError);
                            
                            if (out.isDone()) {
                                return;
                            }
                            out.error(new MediaException(MediaErrorType.Unknown, msg));


                        }
                    }).start();
                }
            });
        } else {
            HTML5Implementation impl = HTML5Implementation.getInstance();
            if (impl.isBacksideHookAvailable()) {
                _log("HTML5Media#playAsyncForBrowsersThatReturnPromise:Adding back-side hook for media. id="+getMediaID(el)+", unlocked="+isUnlocked(el)+", disallowed=true");
                impl.addBacksideHook(new HTML5Implementation.JSRunnable() {
                    @Override
                    public void run() {
                        // IMPORTANT: This is run on the Main thread so we can't do any fancy 
                        // synchronous stuff.
                        final boolean wasUnlocked = isUnlocked(el);
                        
                        _log("HTML5Media#playAsyncForBrowsersThatReturnPromise:Trying to play media inside back-side hook. id="+getMediaID(el)+", unlocked="+wasUnlocked+", disallowed=true");
                        playCatch(el, new NotAllowedHandler() {
                            @Override
                            public void onNotAllowed() {
                                _log("HTML5Media#playAsyncForBrowsersThatReturnPromise:Playing media disallowed inside back-side hook. id="+getMediaID(el)+", unlocked="+wasUnlocked+", disallowed=true");
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        stateListeners.removeListener(onStateChange);
                                        removeMediaErrorListener(onError);
                                        
                                        if (out.isDone()) {
                                            return;
                                        }
                                        out.disallowed = true;
                                        // Apparently we're not allowed to run this, even on the backside hook.
                                        out.error(new MediaException(MediaErrorType.Aborted, "Media was blocked by the browser"));

                                    }

                                }).start();
                            }
                        }, new FailedToPlayHandler() {
                            @Override
                            public void failedToPlay(final String msg) {
                                _log("HTML5Media#playAsyncForBrowsersThatReturnPromise:Failed to play media inside back-side hook. msg="+msg+", id="+getMediaID(el)+", unlocked="+wasUnlocked+", disallowed=true");
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        stateListeners.removeListener(onStateChange);
                                        removeMediaErrorListener(onError);
                                        
                                        if (out.isDone()) {
                                            return;
                                        }
                                        out.error(new MediaException(MediaErrorType.Unknown, msg));
                                    }

                                });
                            }
                        });
                    }
                });
            } else {
                // We've already been disallowed once, and there are no backside hooks available
                // So we'll prompt the user to play the media, which should generate a backside
                // hook that we can use.
                _log("HTML5Media#playAsyncForBrowsersThatReturnPromise:Prompting user to play media because it has been disallowed once, and there are no back-side hooks available. id="+getMediaID(el)+", unlocked="+isUnlocked(el)+", disallowed=true");
                promptUserToPlayMedia(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent t) {
                        _log("HTML5Media#playAsyncForBrowsersThatReturnPromise:User confirmed playing media in prompt. id="+getMediaID(el)+", unlocked="+isUnlocked(el)+", disallowed=true");
                        stateListeners.removeListener(onStateChange);
                        removeMediaErrorListener(onError);
                        if (out.isDone()) {

                            return;
                        }
                        out.initiatedByUserPrompt = true;
                        playAsync(out);
                    }

                }, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent t) {
                        _log("HTML5Media#playAsyncForBrowsersThatReturnPromise:Cancelled playing media at prompt. id="+getMediaID(el)+", unlocked="+isUnlocked(el)+", disallowed=true");
                        stateListeners.removeListener(onStateChange);
                        removeMediaErrorListener(onError);
                        if (out.isDone()) {
                            return;
                        }
                        
                        out.error(new MediaException(MediaErrorType.Aborted, "Play was aborted by the user"));
                    }

                });
            }
        }
        return out;
    }
    
    private PlayRequestInternal playAsyncForBrowsersThatDoNotReturnPromise(final PlayRequestInternal out) {
        // This browser will return a promise from the media play() method
        // so we can detect if play was disallowed.
        if (out.isDone()) {
            return out;
        }
        class StateChangeListener implements ActionListener {
            ActionListener<MediaErrorEvent> onError;
            @Override
            public void actionPerformed(ActionEvent t) {
                if (out.isDone()) {
                    stateListeners.removeListener(this);
                    if (onError != null) {
                        removeMediaErrorListener(onError);
                    }
                    return;
                }
                StateChangeEventInternal evt = (StateChangeEventInternal)t;
                
                if (evt.newState.playing) {
                    stateListeners.removeListener(this);
                    if (onError != null) {
                        removeMediaErrorListener(onError);
                    }
                    out.complete(HTML5Media.this);
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
                if (out.isDone()) {
                    return;
                }
                out.error(t.getMediaException());
            }
            
        };
        onStateChange.onError = onError;
        
        stateListeners.addListener(onStateChange);
        addMediaErrorListener(onError);
        playSafe(el);
        return out;
    }
    
    private PlayRequestInternal playAsync(final PlayRequestInternal out) {
        if (out.isDone()) {
            return out;
        }
        if (!CN.isEdt()) {
            CN.callSerially(new Runnable() {
                public void run() {
                    playAsync(out);
                }
            });
            return out;
        }
        if (el == null) {
            out.error(new IllegalStateException("Attempt to playAsync() media that has already been cleaned up"));
            return out;
        }
        if (pendingPauseRequest != null) {
            pendingPauseRequest.ready(new SuccessCallback<Media>() {
                @Override
                public void onSucess(Media t) {
                    if (!out.isDone()) {
                        playAsync(out);
                    }
                }
            }).except(new SuccessCallback<Throwable>() {
                @Override
                public void onSucess(Throwable t) {
                    if (!out.isDone()) {
                        playAsync(out);
                    }
                }
            });
            pendingPauseRequest = null;
            pendingPlayRequest = out;
            return out;
        }
        if (pendingPlayRequest != null && pendingPlayRequest != out) {
            pendingPlayRequest.ready(new SuccessCallback<Media>() {
                @Override
                public void onSucess(Media t) {
                    if (!out.isDone()) {
                        out.complete(HTML5Media.this);
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
        if (currentState.playing  && !pausePending) {
            // It's already playing
            // If there is a pause pending, then we'll flush this through
            // to override the pending pause as this should hit after the pause.
            out.complete(this);
            return out;
        }
        pendingPlayRequest = out;
        if (!currentState.canPlay) {
            // the media isn't loaded enough to play yet.
            // Let's start the load, and then play when the media can play.
            return loadThenPlay(out);
        }
        
        
        if (playMethodReturnsPromise()) {
            // Most newer browsers return a promise from play() so we're able to handle
            // permissions errors more elegantly.  Because the workflow is different
            // we handle the two types of browsers separately.
            return playAsyncForBrowsersThatReturnPromise(out);
        } else {
            return playAsyncForBrowsersThatDoNotReturnPromise(out);
        }

    }
    
    private MessageEvent currPrompt;
    
    private void promptUserToPlayMedia(final ActionListener onPlay, final ActionListener onCancel) {
        if (!CN.isEdt()) {
            System.out.println("not on EDT in promptUserToPlayMedia:  Redispatching on EDT");
            CN.callSerially(new Runnable() {
                public void run() {
                    promptUserToPlayMedia(onPlay, onCancel);
                }
            });
            return;
        }
        // Let the outside webpage know that we are prompting the user for interaction
        // so that it can display the iframe containing the app if deployed headlessly.
        if (currPrompt != null && !currPrompt.getPromptPromise().isDone()) {
            currPrompt.getPromptPromise().onResult(new AsyncResult<Boolean>() {
                @Override
                public void onReady(Boolean res, Throwable err) {
                    if (err == null && res) {
                        onPlay.actionPerformed(new ActionEvent(null));
                    } else {
                        onCancel.actionPerformed(new ActionEvent(null));
                    }
                    //Window.current().dispatchEvent(createJavascriptPromptCompleteEvent(true));
                }
            });
            return;
        }
        
        Event evt = createJavascriptPromptEvent("MEDIA_READY");
        System.out.println("Prompting user to play media "+this);
        //System.out.println("Request is done? "+out.isDone());
        Window.current().dispatchEvent(evt);
        // Give the app developer a chance to create his own dialog
        PromptPromise result = new PromptPromise();
        final MessageEvent promptEvent = new MessageEvent(result, "Media Ready", 426);
        currPrompt = promptEvent;
        result.onResult(new AsyncResult<Boolean>() {
            @Override
            public void onReady(Boolean res, Throwable err) {
                if (err == null && res) {
                    onPlay.actionPerformed(promptEvent);
                } else {
                    onCancel.actionPerformed(promptEvent);
                }
                Window.current().dispatchEvent(createJavascriptPromptCompleteEvent(true));
            }
        });
        
        
        System.out.println("Dispatching prompt event.  On EDT? "+CN.isEdt());
        Display.getInstance().dispatchMessage(promptEvent);
        if (promptEvent.isConsumed()) {
            // If the app consumed this message event, that means it is handling the prompt
            // we can just return
            System.out.println("Prompt event was consumed.");
            return;
        }
        System.out.println("Prompt event was not consumed.  Showing popup prompt");
        
        currPrompt = null;
        if (playMediaSheet != null) {
            playMediaSheet.back();
            playMediaSheet = null;
        }
        final boolean[] playButtonPressed = new boolean[1];
        final Sheet sheet = new Sheet(Sheet.getCurrentSheet(), "Media Ready");
        Button playButton = new Button("Play Now");
        playButton.setMaterialIcon(FontImage.MATERIAL_PLAY_ARROW);
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                onPlay.actionPerformed(arg0);
                playButtonPressed[0] = true;
                sheet.back();
                Window.current().dispatchEvent(createJavascriptPromptCompleteEvent(true));
            }

        });
        sheet.addCloseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent t) {

                if (!playButtonPressed[0]) {
                    onCancel.actionPerformed(t);
                    Window.current().dispatchEvent(createJavascriptPromptCompleteEvent(false));
                }
            }

        });
        sheet.getContentPane().setLayout(BoxLayout.y());
        sheet.getContentPane().add(FlowLayout.encloseCenter(playButton));
        playMediaSheet = sheet;
        sheet.show();
    }
    
    
    @JSBody(params={"description"}, script="return new CustomEvent('cn1userprompt', {detail: description})")
    private native static Event createJavascriptPromptEvent(String description);
    
    @JSBody(params={"response"}, script="return new CustomEvent('cn1userpromptresponse', {detail: response})")
    private native static Event createJavascriptPromptCompleteEvent(boolean response);
    
   
    @JSBody(params={"el"}, script="try{ el.load();} catch (e) {console.log(e);}")
    private static native void loadMedia(HTMLMediaElement el);
    
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
        if (el == null) {
            out.error(new IllegalStateException("Media has already been cleaned up"));
            return out;
        }
        if (pendingPlayRequest != null) {
            pendingPlayRequest.ready(new SuccessCallback<Media>() {
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
            pendingPlayRequest = null;
            pendingPauseRequest = out;
            return out;
        }
        if (pendingPauseRequest != null && pendingPauseRequest != out) {
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
        pendingPauseRequest = out;
        if (currentState.paused) {
            // It's already playing
            // If there is a pause pending, then we'll flush this through
            // to override the pending pause as this should hit after the pause.
            out.complete(this);
            return out;
        }
        
        final ActionListener onStateChange = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent t) {
                if (out.isDone()) {
                    stateListeners.removeListener(this);
                    return;
                }
                StateChangeEventInternal evt = (StateChangeEventInternal)t;
                
                if (evt.newState.paused) {
                    stateListeners.removeListener(this);
                    out.complete(HTML5Media.this);
                    return;
                }
            }

        };
        stateListeners.addListener(onStateChange);
        //el.pause();
        pauseNative(el);
        return out;
    }

    @Override
    protected void pauseImpl() {
        throw new RuntimeException("Shouldn't need this because we override pauseAsync()");
    }

    @Override
    protected void playImpl() {
        throw new RuntimeException("Shouldn't need this because we override playAsync()");
    }
    
    @JSBody(params={"el"}, script="if (window.cn1DebugPauseFunction) window.cn1DebugPauseFunction(el); else el.pause();")
    private static native void pauseNative(HTMLMediaElement el);
    

    @Override
    public PauseRequest pauseAsync() {
        
        final PauseRequest out = new PauseRequest();
        if (!CN.isEdt()) {
            Log.e(new IllegalStateException("WARNING: Calling Media.pauseAsync off the EDT"));
            
        }
        pauseAsyncInternal().ready(new SuccessCallback<Media>() {
            @Override
            public void onSucess(Media t) {
                if (el == null) {
                    out.error(new IllegalStateException("Attempt to pause media that is already cleaned up"));
                    return;
                }
                out.complete(HTML5Media.this);
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable t) {
                out.error(t);
            }
        });
        return out;
    }

    @Override
    public PlayRequest playAsync() {
        if (!CN.isEdt()) {
           Log.e(new IllegalStateException("WARNING: Calling Media.playAsync off the EDT"));
        }
        final PlayRequest out = new PlayRequest();
        if (el == null) {
            out.error(new IllegalStateException("Attempt to play media that is already cleaned up."));
            return out;
        }
        playAsyncInternal().ready(new SuccessCallback<Media>() {
            @Override
            public void onSucess(Media t) {
                if (!out.isDone()) {
                    if (el == null) {
                        out.error(new IllegalStateException("Attempt to play media that is already cleaned up"));
                        return;
                    }
                    out.complete(HTML5Media.this);
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
    
    

    @Override
    public int getTime() {
        if (el == null || currentState == null || !currentState.canPlay) {
            return -1;
        }
        
        return (int)(el.getCurrentTime() * 1000);
    }

    @Override
    public int getDuration() {
        if (el == null || currentState == null || !currentState.canPlay) {
            return -1;
        }
        
        return (int)(el.getDuration() * 1000);
    }

    @Override
    public boolean isPlaying() {
        if (el == null) {
            return false;
        }
        
        return currentState.playing;
    }

    @Override
    public int getVolume() {
        if (el == null) {
            return 0;
        }
        
        return (int)(el.getVolume()*100.0);
    }

    private boolean shouldSetMuteOnZeroVolume() {
        return HTML5Implementation.isIOS() && "true".equals(CN.getProperty("javascript.iosMuteOnZeroVolume", "true"));
    }
    
    @Override
    public void setVolume(int volume) {
        if (el == null) {
            return;
        }
        if (volume == 0 && shouldSetMuteOnZeroVolume()) {
            el.setMuted(true);
        } else if (shouldSetMuteOnZeroVolume()) {
            el.setMuted(false);
        }
        el.setVolume(volume/100.0);
    }

    @Override
    public void setTime(int time) {
        pendingTime = time;
        if (el == null) {
            return;
        }
        el.setCurrentTime(time/1000.0);
        waitWhileSeeking(5000);

    }

    @Override
    public void setVariable(String string, Object o) {
        if (el == null) {
            return;
        }
        if (Media.VARIABLE_NATIVE_CONTRLOLS_EMBEDDED.equals(string) && o instanceof Boolean) {
            el.setControls((Boolean)o);
        }
    }

    @Override
    public Object getVariable(String string) {
        return null;
    }

    
     @Override
    public void prepare() {
        if (el != null) {
            loadMedia(el);
        }
    }

    @Override
    public void cleanup() {
        _log("In Media.cleanup");
        
        if (el != null) {
            //el.pause();
            pauseNative(el);
            HTMLMediaElement tmp = el;
            el = null;
            HTML5Implementation.getInstance().mediaPool().returnMediaElement(tmp);
        }
    }

    @Override
    public void setNativePlayerMode(boolean bln) {

    }

    @Override
    public boolean isNativePlayerMode() {
        return false;
    }

    @Override
    public boolean isFullScreen() {
        if (el == null) {
            return false;
        }
        if (isVideo) {
            return el.hasAttribute("playsinline");
            //return ((HTMLVideoElement)el).isDisplayingFullscreen();
        }
        return false;   
    }

    @Override
    public void setFullScreen(boolean full) {
        if (el == null) {
            return;
        }
        if (isVideo) {
            HTMLVideoElement videoEl = (HTMLVideoElement)el;

            if (full){
                videoEl.removeAttribute("playsinline");
                videoEl.enterFullscreen();
            } else {
                videoEl.setAttribute("playsinline", "");
                videoEl.exitFullscreen();
            }
        }
    }

    @Override
    public boolean isVideo() {
        return isVideo;
    }
    
    
    public class MediaComponent extends HTML5Peer {

        public MediaComponent() {
            super(HTML5Media.this.el);
        }
        
        
        @Override
        protected void initComponent() {
            super.initComponent(); 
            //el.getOwnerDocument().getBody().appendChild(el);

        }

        @Override
        protected void deinitialize() {
            super.deinitialize();
            //el.getOwnerDocument().getBody().removeChild(el);
        }
    }

    private boolean isSeeking() {
        if (HTML5Media.this.el == null) return false;
        return HTML5Media.this.el.isSeeking();
    }

    private void waitWhileSeeking(final long timeout) {
        if (!isSeeking()) return;
        final Runnable waitRunnable = new Runnable() {
            public void run() {
                long timeOutTime = System.currentTimeMillis() + timeout;
                while (timeOutTime > System.currentTimeMillis() && isSeeking()) {
                    Util.sleep(50);
                }
            }
        };
        if (CN.isEdt()) {
            CN.invokeAndBlock(waitRunnable);
        } else {
            waitRunnable.run();
        }
    }
    
    
    private static boolean isUnlocked(HTMLMediaElement el) {
        return HTML5MediaPool.isUnlocked(el);
    }
    
    private static String getMediaID(HTMLMediaElement el) {
        return HTML5MediaPool.getMediaID(el);
    }
}
