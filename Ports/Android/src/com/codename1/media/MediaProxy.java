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

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionListener;
import com.codename1.util.SuccessCallback;

/**
 *
 * @author Chen
 */
public class MediaProxy extends AbstractMedia {

    private Media media;
    private AsyncMedia asyncMedia;
    
    private ActionListener<MediaStateChangeEvent> stateChangeListener = new ActionListener<MediaStateChangeEvent> () {
        
        @Override
        public void actionPerformed(MediaStateChangeEvent evt) {
            fireMediaStateChange(evt.getNewState());
        }
        
    };
    
    private ActionListener<MediaErrorEvent> errorListener = new ActionListener<MediaErrorEvent>() {
        @Override
        public void actionPerformed(MediaErrorEvent evt) {
            fireMediaError(evt.getMediaException());
        }
        
    };
    
    public MediaProxy(Media m) {
        this.media = m;
        this.asyncMedia = MediaManager.getAsyncMedia(m);
        asyncMedia.addMediaStateChangeListener(stateChangeListener);
        asyncMedia.addMediaErrorListener(errorListener);
    }
    
    
    @Override
    protected void playImpl() {
        media.play();
    }

    @Override
    public PlayRequest playAsync() {
        final PlayRequest out = new PlayRequest();
        PlayRequest req = asyncMedia.playAsync();
        req.ready(new SuccessCallback<AsyncMedia>() {
            @Override
            public void onSucess(AsyncMedia value) {
                out.complete(MediaProxy.this);
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable value) {
                out.error(value);
            }
        });
        
        return out;
        
    }
    
    

    @Override
    protected void pauseImpl() {
        media.pause();
    }

    @Override
    public PauseRequest pauseAsync() {
        final PauseRequest out = new PauseRequest();
        PauseRequest req = asyncMedia.pauseAsync();
        req.ready(new SuccessCallback<AsyncMedia>() {
            @Override
            public void onSucess(AsyncMedia value) {
                out.complete(MediaProxy.this);
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable value) {
                out.error(value);
            }
        });
        
        return out;
    }
    
    

    @Override
    public void prepare() {
        media.prepare();
    }

    @Override
    public void cleanup() {
        media.cleanup();
        asyncMedia.removeMediaStateChangeListener(stateChangeListener);
    }

    @Override
    public int getTime() {
        return media.getTime();
    }

    @Override
    public void setTime(int time) {
        media.setTime(time);
    }

    @Override
    public int getDuration() {
        return media.getDuration();
    }

    @Override
    public void setVolume(int vol) {
        media.setVolume(vol);
    }

    @Override
    public int getVolume() {
        return media.getVolume();
    }

    @Override
    public boolean isPlaying() {
        return media.isPlaying();
    }

    @Override
    public Component getVideoComponent() {
        return media.getVideoComponent();
    }

    @Override
    public boolean isVideo() {
        return media.isVideo();
    }

    @Override
    public boolean isFullScreen() {
        return media.isFullScreen();
    }

    @Override
    public void setFullScreen(boolean fullScreen) {
        media.setFullScreen(fullScreen);
    }

    @Override
    public void setNativePlayerMode(boolean nativePlayer) {
        media.setNativePlayerMode(nativePlayer);
    }

    @Override
    public boolean isNativePlayerMode() {
        return media.isNativePlayerMode();
    }

    @Override
    public void setVariable(String key, Object value) {
        media.setVariable(key, value);
    }

    @Override
    public Object getVariable(String key) {
        return media.getVariable(key);
    }
    
    
    public void addCompletionHandler(Runnable onCompletion) {
        Display.getInstance().addCompletionHandler(media, onCompletion);
    }
    
    public void removeCompletionHandler(Runnable onCompletion) {
        Display.getInstance().removeCompletionHandler(media, onCompletion);
    }
}
