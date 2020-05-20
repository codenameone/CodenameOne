/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui.events;
import com.codename1.ui.Display;

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
}
