package com.codename1.samples;


import static com.codename1.ui.CN.*;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.io.Log;
import com.codename1.ui.Toolbar;
import java.io.IOException;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.io.NetworkEvent;

/**
 * This file was generated by <a href="https://www.codenameone.com/">Codename One</a> for the purpose 
 * of building native mobile applications using Java.
 */
public class TestParparTranspileError2922 {

    private Form current;
    private Resources theme;

    public void init(Object context) {
        // use two network threads instead of one
        updateNetworkThreadCount(2);

        theme = UIManager.initFirstTheme("/theme");

        // Enable Toolbar on all Forms by default
        Toolbar.setGlobalToolbar(true);

        // Pro only feature
        Log.bindCrashProtection(true);

        addNetworkErrorListener(err -> {
            // prevent the event from propagating
            err.consume();
            if(err.getError() != null) {
                Log.e(err.getError());
            }
            Log.sendLogAsync();
            Dialog.show("Connection Error", "There was a networking error in the connection to " + err.getConnectionRequest().getUrl(), "OK", null);
        });        
    }
    
    public void start() {
        if(current != null){
            current.show();
            return;
        }
        Form hi = new Form("Test Split", BoxLayout.y());
        String text = "Abc-123456-7890";
        String[] tokens = split(text, '-');
        hi.add(new Label("Original string: " + text));
        hi.add(new Label("Token 1: " + tokens[0]));
        hi.add(new Label("Token 2: " + tokens[1]));
        hi.show();
    }
    
    /**
     * Similar to tokenize, but in this case it splits the string always in two
     * tokens, according to the first occurence of the given separator.
     *
     * @param text
     * @param separator
     * @return a string array of two tokens: if the saparator is not found, the
     * first token is equal to the given text and the second token is an empty
     * string
     */
    public static String[] split(String text, Character separator) {
        String[] result = {"", ""};
        boolean separatorFound = false;
        for (Character character : text.toCharArray()) {
            if (!separatorFound && character.equals(separator)) {
                separatorFound = true;
                continue;
            }
            if (!separatorFound) {
                result[0] += character;
            } else {
                result[1] += character;
            }
        }
        return result;
    }

    public void stop() {
        current = getCurrentForm();
        if(current instanceof Dialog) {
            ((Dialog)current).dispose();
            current = getCurrentForm();
        }
    }
    
    public void destroy() {
    }

}
