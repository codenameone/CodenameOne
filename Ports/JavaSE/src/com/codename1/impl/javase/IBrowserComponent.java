/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase;

/**
 *
 * @author shannah
 */
public interface IBrowserComponent {
    void back();
    public void forward();
    void setPage(String html, String baseUrl);
    String getTitle();

    String getURL();

    void setURL(String url);

    void stop();

    void reload();

    boolean hasBack();
    void clearHistory();
    boolean hasForward();
    public void execute(final String js);
    public String executeAndReturnString(final String js);
    public void setProperty(String key, Object value);
    
    public void runLater(Runnable r);
    void exposeInJavaScript(Object o, String name);
    public boolean supportsExecuteAndReturnString();
    
}
