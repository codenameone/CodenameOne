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
package com.codename1.ui.events;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

/**
 * Encapsulates an event that either originates outside the App (e.g. from the webpage that 
 * contains the app if it is published using the Javascript port); or whose destination is
 * outside the app.
 * 
 * @author shannah
 * @since 7.0
 * @see Display#postMessage(com.codename1.ui.events.MessageEvent) 
 * @see Display#dispatchMessage(com.codename1.ui.events.MessageEvent) 
 * @see Display#addMessageListener(com.codename1.ui.events.ActionListener) 
 * @see Display#removeMessageListener(com.codename1.ui.events.ActionListener) 
 */
public class MessageEvent extends ActionEvent {
    private final String message;
    private final int code;
    
    /**
     * Creates a new message.
     * @param source The source of the message.
     * @param message The message content.
     * @param code A code for the message.
     */
    public MessageEvent(Object source, String message, int code) {
        super(source);
        this.message = message;
        this.code = code;
    }
    
    /**
     * Gets the message content.
     * @return The message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Gets the message code.
     * @return 
     */
    public int getCode() {
        return code;
    }
    
    /**
     * Checks to see if this message is a prompt to record audio.  This is currently only used 
     * in the Javascript port, and it allows you to implement a custom permissions prompt to
     * record audio.
     * 
     * <p>See <a href="https://shannah.github.io/cn1-recipes/#_displaying_custom_prompt_to_play_audio">Displaying Custom Prompt to Play Audio</a> for an example.</p>
     * 
     * @return True if this message is a prompt for the audio recorder.
     */
    public boolean isPromptForAudioRecorder() {
        return getCode() == 427 && getSource() instanceof PromptPromise;
    }
    
    /**
     * Checks to see if this message is a prompt to play audio.  This is currently only used 
     * in the Javascript port, and it allows you to implement a custom permissions prompt to 
     * play audio.
     * 
     * <p>See <a href="https://shannah.github.io/cn1-recipes/#_displaying_custom_prompt_to_play_audio">Displaying Custom Prompt to Play Audio</a> for an example.</p>
     * 
     * @return True if this message is a prompt for the audio player.
     */
    public boolean isPromptForAudioPlayer() {
        return getCode() == 426 && getSource() instanceof PromptPromise;
    }
    
    /**
     * This obtains the "promise" that should be fulfilled if implementing a custom permissions prompt
     * for playing or recording audio.  Currently this is only used for the Javascript port.
     * 
     * <p>See <a href="https://shannah.github.io/cn1-recipes/#_displaying_custom_prompt_to_play_audio">Displaying Custom Prompt to Play Audio</a> for an example.</p>
     * @return The promise to be fulfilled, or null if this event is not a prompt.
     * @see #isPromptForAudioPlayer() 
     * @see #isPromptForAudioRecorder() 
     */
    public PromptPromise getPromptPromise() {
        if (getSource() instanceof PromptPromise) {
            return (PromptPromise)getSource();
        }
        return null;
    }
    
    /**
     * Encapsulates a promise that should be fulfilled when implementingn a custom permissions
     * prompt to record or play audio. Currently this is only used for the Javscript port.
     * 
     * <p>See <a href="https://shannah.github.io/cn1-recipes/#_displaying_custom_prompt_to_play_audio">Displaying Custom Prompt to Play Audio</a> for an example.</p>
     */
    public static class PromptPromise extends AsyncResource<Boolean> {
        
    }
}
