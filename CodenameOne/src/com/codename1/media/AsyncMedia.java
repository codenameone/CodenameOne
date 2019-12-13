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

import com.codename1.io.Log;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.util.AsyncResource;

/**
 * An interface for media elements that provides asynchronous pause and play functionality
 * as well as support for state change events, so that interested parties can register
 * to be notified when state changes between play and pause.
 * 
 * You can convert any {@link Media} object into an AsyncMedia object using {@link MediaManager#getAsyncMedia(com.codename1.media.Media) }.
 * In most cases this just casts the object to AsyncMedia, since most media returned from {@link MediaManager} already implement
 * this interface.  In cases where the media doesn't already implement AsyncMedia, it will return an Async wrapper around the media.
 * @author shannah
 * @since 7.0
 * @see MediaManager#getAsyncMedia(com.codename1.media.Media) 
 */
public interface AsyncMedia extends Media {
    
    /**
     * An enum to represent the state of a media object.
     */
    public static enum State {
        Playing,
        Paused
    }
    
    /**
     * Encapsulates a state-change event on a Media object.
     * @since 7.0
     */
    public static class MediaStateChangeEvent extends ActionEvent {

        /**
         * The previous state.
         * @return the oldState
         */
        public State getOldState() {
            return oldState;
        }

        /**
         * The new state.
         * @return the newState
         */
        public State getNewState() {
            return newState;
        }
        private State oldState;
        private State newState;
        
        /**
         * Creates a new state change event for the given source.
         * @param source The media object whose state is changing.
         * @param oldState The old state
         * @param newState The new state
         */
        public MediaStateChangeEvent(AsyncMedia source, State oldState, State newState) {
            super(source);
            this.oldState = oldState;
            this.newState = newState;
        }
    }
    
    /**
     * Enum encapsulating the different types of media errors that can occur.
     * @since type.
     */
    public static enum MediaErrorType {
        /**
         * The fetching of the associated resource was aborted by the user's request.
         */
        Aborted("The fetching of the associated resource was aborted by the user's request"),
        
        /**
         * Some kind of network error occurred which prevented the media from being successfully fetched, despite having previously been available.
         */
        Network("Some kind of network error occurred which prevented the media from being successfully fetched, despite having previously been available."),
        
        /**
         * Despite having previously been determined to be usable, an error occurred while trying to decode the media resource, resulting in an error.
         */
        Decode("Despite having previously been determined to be usable, an error occurred while trying to decode the media resource, resulting in an error."),
        Encode("Failed to encode media to given type"),
        /**
         * The associated resource has been found to be unsuitable.
         */
        SrcNotSupported("The associated resource has been found to be unsuitable."),
        
        Unknown("Unknown error"),
        LineUnavailable("The associated input line is unavailable");
        
        private String description;
        
        MediaErrorType(String description) {
            this.description = description;
        }
    }
    
    /**
     * Encapsulates a media exception.
     * @since 7.0
     */
    public static class MediaException extends RuntimeException {
        private MediaErrorType mediaErrorType;
        
        /**
         * Creates an exception of the given type.
         * @param type The type of exception
         */
        public MediaException(MediaErrorType type) {
            super(type.description);
            this.mediaErrorType = type;
        }
        
        /**
         * Creates an exception of the given type.
         * @param type THe type of error.
         * @param message The error message.
         */
        public MediaException(MediaErrorType type, String message) {
            super(message);
            this.mediaErrorType = type;
        }
        
        /**
         * Creates an exception of the given type.
         * @param type The type of error
         * @param cause An underlying exception that caused this error.
         */
        public MediaException(MediaErrorType type, Throwable cause) {
            super(cause.getMessage());
            Log.e(cause);
            this.mediaErrorType = type;
        }
        
        /**
         * Gets the error type.
         * @return 
         */
        public MediaErrorType getMediaErrorType() {
            return mediaErrorType;
        }
    }
    
    /**
     * Encapsulates a media error event.
     * @since 7.0
     */
    public static class MediaErrorEvent extends ActionEvent {
        private MediaException mediaException;
        
        /**
         * Creates a new error event with the given exception.
         * @param source
         * @param error 
         */
        public MediaErrorEvent(Media source, MediaException error) {
            super(source);
            this.mediaException = error;
        }
        
        /**
         * Gets the exception associated with this event.
         * @return 
         */
        public MediaException getMediaException() {
            return mediaException;
        }
    }
    
    /**
     * An async resource used to track the progress of a playAsync() request.  It will
     * resolve when the media has started playing, or if an error occurs first.
     * @see #playAsync() 
     * @since 7.0
     */
    public static class PlayRequest extends AsyncResource<AsyncMedia> {

    }
    
    /**
     * An async resource used to track the progress of a pauseAsync() request.  It will
     * resolve when the media has paused, or if an error occurs first.
     * @see #pauseAsync() 
     */
    public static class PauseRequest extends AsyncResource<AsyncMedia> {
        
    }
    
    /**
     * Gets the current state of the media object.
     * @return The state of the media object.
     */
    public State getState();
    
    /**
     * Adds a listener to be notified when the state of the media changes.
     * @param l Listener
     */
    public void addMediaStateChangeListener(ActionListener<MediaStateChangeEvent> l);
    
    /**
     * Removes a listener so that it will no longer be notified of state changes.
     * @param l Listener
     */
    public void removeMediaStateChangeListener(ActionListener<MediaStateChangeEvent> l);
    
    /**
     * Adds a listener to be notified when an error occurs in the media.
     * @param l Listener
     */
    public void addMediaErrorListener(ActionListener<MediaErrorEvent> l);
    
    /**
     * Removes a listener so that it will no longer be notified of errors.
     * @param l Listener
     */
    public void removeMediaErrorListener(ActionListener<MediaErrorEvent> l);
    
    /**
     * Adds a callback to be run when the media has played to completion.
     * @param onComplete 
     */
    public void addMediaCompletionHandler(Runnable onComplete);
    
    
    /**
     * Initiates a play request.  Returns immediately without blocking.  Caller can use
     * the returned PlayRequest object to be notified when playing has actually started.
     * @return 
     */
    public PlayRequest playAsync();
    
    /**
     * Initiates a pause request.  Returns immediately without blocking.  Caller can use
     * the returned PauseRequest object to be notified when media has actually paused.
     * @return 
     */
    public PauseRequest pauseAsync();
}
