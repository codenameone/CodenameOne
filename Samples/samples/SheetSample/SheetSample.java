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
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Sheet;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.layouts.BorderLayout;

/**
 * Sample using Sheet class.
 * <p><a href="https://youtu.be/3okEj_JW3-k">Screen cast of the SheetSample demo</a></p>
 */
public class SheetSample {

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
        Form hi = new Form("Hi World", new BorderLayout());


        Button b = new Button("Open Sheet");
        b.addActionListener(e->{
            new MySheet(null).show();
            
            
        });
        hi.add(BorderLayout.NORTH, b);
        hi.show();
    }
    
    private class MySheet extends Sheet {
        MySheet(Sheet parent) {
            super(parent, "My Sheet");
            Container cnt = getContentPane();
            cnt.setLayout(BoxLayout.y());
            Button gotoSheet2 = new Button("Goto Sheet 2");
            gotoSheet2.addActionListener(e->{
                new MySheet2(this).show(300);
            });
            cnt.add(gotoSheet2);
            for (String t : new String[]{"Red", "Green", "Blue", "Orange"}) {
                cnt.add(new Label(t));
            }
        }
    }
    
    private class MySheet2 extends Sheet {
        MySheet2(Sheet parent) {
            super(parent, "Sheet 2");
            Container cnt = getContentPane();
            cnt.setLayout(BoxLayout.y());
            cnt.setScrollableY(true);
            for (int i=0; i<2; i++) {
                for (String t : new String[]{"Red", "Green", "Blue", "Orange"}) {
                    cnt.add(new Label(t));
                }
            }
        }
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
