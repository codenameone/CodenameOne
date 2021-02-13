/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.builders.util;



/**
 * A copy from com.codename1.io so that we don't need to add CodenameOne as a 
 * dependency to the Offline builder.
 */
public interface JSONParseCallback {
    /**
     * Indicates that the parser ran into an opening bracket event {
     */
    public void startBlock(String blockName);

    /**
     * Indicates that the parser ran into an ending bracket event }
     */
    public void endBlock(String blockName);

    /**
     * Indicates that the parser ran into an opening bracket event [
     */
    public void startArray(String arrayName);

    /**
     * Indicates that the parser ran into an ending bracket event ]
     */
    public void endArray(String arrayName);

    /**
     * Submits a token from the JSON data as a java string, this token is always a string value
     */
    public void stringToken(String tok);

    /**
     * Submits a numeric token from the JSON data
     * @param tok the token value
     */
    public void numericToken(double tok);

    /**
     * Submits a boolean token from the JSON data
     * @param tok the token value
     */
    public void booleanToken(boolean tok);
    
    /**
     * Submits a numeric token from the JSON data
     */
    public void longToken(long tok);

    /**
     * This method is called when a string key/value pair is detected within the json
     * it is essentially redundant when following string/numeric token.
     *
     * @param key the key
     * @param value a string value
     */
    public void keyValue(String key, String value);

    /**
     * This method indicates to the Parser if this Callback is still alive
     * 
     * @return true if the Callback is still interested to get the JSON parse
     * events from the JSONParser
     */
    public boolean isAlive();
}
