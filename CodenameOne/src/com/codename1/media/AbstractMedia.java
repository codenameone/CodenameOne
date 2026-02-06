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
package com.codename1.media;

import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.util.SuccessCallback;

/// An abstract base class for AsyncMedia. Most media returned from `MediaManager` will
/// be descendants of this class.
///
/// @author shannah
///
/// #### Since
///
/// 7.0
public abstract class AbstractMedia implements AsyncMedia {
    private final EventDispatcher stateChangeListeners = new EventDispatcher();
    private final EventDispatcher errorListeners = new EventDispatcher();

    /// Currently pending play request
    private PlayRequest pendingPlayRequest;

    /// Currently pending pause request.
    private PauseRequest pendingPauseRequest;

    /// {@inheritDoc }
    @Override
    public State getState() {
        if (isPlaying()) {
            return State.Playing;
        } else {
            return State.Paused;
        }
    }

    /// Fires a media state change event to the registered state change listeners.
    ///
    /// #### Parameters
    ///
    /// - `newState`: The new state
    ///
    /// #### Returns
    ///
    /// The state change event.
    protected MediaStateChangeEvent fireMediaStateChange(State newState) {
        MediaStateChangeEvent evt = new MediaStateChangeEvent(this, getState(), newState);
        if (stateChangeListeners.hasListeners()) {
            stateChangeListeners.fireActionEvent(evt);
        }
        return evt;
    }

    /// {@inheritDoc }
    @Override
    public void addMediaStateChangeListener(ActionListener<MediaStateChangeEvent> l) {
        stateChangeListeners.addListener(l);
    }

    /// {@inheritDoc }
    @Override
    public void removeMediaStateChangeListener(ActionListener<MediaStateChangeEvent> l) {
        stateChangeListeners.removeListener(l);
    }

    /// Fires a media error event to registered listeners.
    ///
    /// #### Parameters
    ///
    /// - `ex`: The MediaException to deliver
    ///
    /// #### Returns
    ///
    /// The MediaErrorEvent object sent to listeners.
    ///
    /// #### See also
    ///
    /// - #addMediaErrorListener(com.codename1.ui.events.ActionListener)
    ///
    /// - #removeMediaErrorListener(com.codename1.ui.events.ActionListener)
    protected MediaErrorEvent fireMediaError(MediaException ex) {
        MediaErrorEvent evt = new MediaErrorEvent(this, ex);
        if (errorListeners.hasListeners()) {
            errorListeners.fireActionEvent(evt);
        }
        return evt;
    }

    /// {@inheritDoc }
    @Override
    public void addMediaErrorListener(ActionListener<MediaErrorEvent> l) {
        errorListeners.addListener(l);
    }

    /// {@inheritDoc }
    @Override
    public void removeMediaErrorListener(ActionListener<MediaErrorEvent> l) {
        errorListeners.removeListener(l);
    }

    /// {@inheritDoc }
    @Override
    public void addMediaCompletionHandler(Runnable onComplete) {
        MediaManager.addCompletionHandler(this, onComplete);
    }

    /// {@inheritDoc }
    @Override
    public PlayRequest playAsync() {
        return playAsync(new PlayRequest() {
            @Override
            public void complete(AsyncMedia value) {
                if (this == pendingPlayRequest) { //NOPMD CompareObjectsWithEquals
                    pendingPlayRequest = null;
                }
                super.complete(value);
            }

            @Override
            public void error(Throwable t) {
                if (this == pendingPlayRequest) { //NOPMD CompareObjectsWithEquals
                    pendingPlayRequest = null;
                }
                super.error(t);
            }
        });
    }


    private PlayRequest playAsync(final PlayRequest out) {
        if (out.isDone()) {
            return out;
        }

        if (pendingPauseRequest != null) {
            pendingPauseRequest.ready(new SuccessCallback<AsyncMedia>() {
                @Override
                public void onSucess(AsyncMedia value) {
                    if (!out.isDone()) {
                        playAsync(out);
                    }
                }
            }).except(new SuccessCallback<Throwable>() {
                @Override
                public void onSucess(Throwable value) {
                    if (!out.isDone()) {
                        playAsync(out);
                    }
                }
            });
            pendingPauseRequest = null;
            pendingPlayRequest = out;
            return out;
        }

        if (pendingPlayRequest != null && pendingPlayRequest != out) { //NOPMD CompareObjectsWithEquals
            pendingPlayRequest.ready(new PlayAsyncSuccessCallback(out))
                    .except(new PlayAsyncExceptSuccessCallback(out));
            return out;
        } else {
            pendingPlayRequest = out;
        }

        if (getState() == State.Playing) {
            out.complete(this);
            return out;
        }

        class StateChangeListener implements ActionListener<MediaStateChangeEvent> {
            ActionListener<MediaErrorEvent> onError;

            @Override
            public void actionPerformed(MediaStateChangeEvent evt) {
                if (!out.isDone()) {
                    if (evt.getNewState() == State.Playing) {
                        stateChangeListeners.removeListener(this);
                        if (onError != null) {
                            errorListeners.removeListener(onError);
                        }
                        out.complete(AbstractMedia.this);
                    }
                }

            }

        }
        final StateChangeListener onStateChange = new StateChangeListener();
        ActionListener<MediaErrorEvent> onError = new ActionListener<MediaErrorEvent>() {
            @Override
            public void actionPerformed(MediaErrorEvent evt) {
                stateChangeListeners.removeListener(onStateChange);
                errorListeners.removeListener(this);
                if (!out.isDone()) {
                    out.error(evt.getMediaException());
                }
            }

        };
        onStateChange.onError = onError;
        stateChangeListeners.addListener(onStateChange);
        errorListeners.addListener(onError);
        playImpl();

        return out;

    }

    /// {@inheritDoc }
    @Override
    public PauseRequest pauseAsync() {
        return pauseAsync(new PauseRequest() {
            @Override
            public void complete(AsyncMedia value) {
                if (pendingPauseRequest == this) { //NOPMD CompareObjectsWithEquals
                    pendingPauseRequest = null;
                }
                super.complete(value);
            }

            @Override
            public void error(Throwable t) {
                if (pendingPauseRequest == this) { //NOPMD CompareObjectsWithEquals
                    pendingPauseRequest = null;
                }
                super.error(t);
            }


        });

    }

    private PauseRequest pauseAsync(final PauseRequest out) {
        if (out.isDone()) {
            return out;
        }


        if (pendingPlayRequest != null) {
            pendingPlayRequest.ready(new SuccessCallback<AsyncMedia>() {
                @Override
                public void onSucess(AsyncMedia value) {
                    if (!out.isDone()) {
                        pauseAsync(out);
                    }
                }
            }).except(new SuccessCallback<Throwable>() {
                @Override
                public void onSucess(Throwable value) {
                    if (!out.isDone()) {
                        pauseAsync(out);
                    }
                }
            });
            pendingPlayRequest = null;
            pendingPauseRequest = out;
            return out;
        }
        if (pendingPauseRequest != null && pendingPauseRequest != out) { //NOPMD CompareObjectsWithEquals
            pendingPauseRequest.ready(new PauseAsyncSuccessCallback(out))
                    .except(new PauseAsyncExceptSuccessCallback(out));
            return out;
        } else {
            pendingPauseRequest = out;
        }

        if (getState() == State.Paused) {
            out.complete(this);
            return out;
        }

        class StateChangeListener implements ActionListener<MediaStateChangeEvent> {
            ActionListener<MediaErrorEvent> onError;

            @Override
            public void actionPerformed(MediaStateChangeEvent evt) {

                if (!out.isDone()) {
                    if (evt.getNewState() == State.Paused) {
                        stateChangeListeners.removeListener(this);
                        if (onError != null) {
                            errorListeners.removeListener(onError);
                        }
                        out.complete(AbstractMedia.this);
                    }
                }

            }

        }

        final StateChangeListener onStateChange = new StateChangeListener();
        ActionListener<MediaErrorEvent> onError = new ActionListener<MediaErrorEvent>() {
            @Override
            public void actionPerformed(MediaErrorEvent evt) {
                stateChangeListeners.removeListener(onStateChange);
                errorListeners.removeListener(this);
                if (!out.isDone()) {
                    out.error(evt.getMediaException());
                }
            }

        };
        onStateChange.onError = onError;
        stateChangeListeners.addListener(onStateChange);
        errorListeners.addListener(onError);
        pauseImpl();

        return out;
    }

    /// Initiates a play request on the media.
    protected abstract void playImpl();

    /// Initiates a pause request on the media.
    protected abstract void pauseImpl();

    /// {@inheritDoc }
    @Override
    public final void play() {
        playAsync();
    }

    /// {@inheritDoc }
    @Override
    public final void pause() {
        pauseAsync();
    }

    private static class PauseAsyncSuccessCallback implements SuccessCallback<AsyncMedia> {
        private final PauseRequest out;

        public PauseAsyncSuccessCallback(PauseRequest out) {
            this.out = out;
        }

        @Override
        public void onSucess(AsyncMedia value) {
            if (!out.isDone()) {
                out.complete(value);
            }
        }
    }

    private static class PauseAsyncExceptSuccessCallback implements SuccessCallback<Throwable> {
        private final PauseRequest out;

        public PauseAsyncExceptSuccessCallback(PauseRequest out) {
            this.out = out;
        }

        @Override
        public void onSucess(Throwable value) {
            if (!out.isDone()) {
                out.error(value);
            }
        }
    }

    private static class PlayAsyncSuccessCallback implements SuccessCallback<AsyncMedia> {
        private final PlayRequest out;

        public PlayAsyncSuccessCallback(PlayRequest out) {
            this.out = out;
        }

        @Override
        public void onSucess(AsyncMedia value) {
            if (!out.isDone()) {
                out.complete(value);
            }
        }
    }

    private static class PlayAsyncExceptSuccessCallback implements SuccessCallback<Throwable> {
        private final PlayRequest out;

        public PlayAsyncExceptSuccessCallback(PlayRequest out) {
            this.out = out;
        }

        @Override
        public void onSucess(Throwable value) {
            if (!out.isDone()) {
                out.error(value);
            }
        }
    }
}
