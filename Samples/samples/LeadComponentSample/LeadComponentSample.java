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
import com.codename1.ui.Component;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;

/**
 * This demonstrates the use of a LeadComponent to have a Button handle all of the
 * pointer events for its parent container.
 */
public class LeadComponentSample {

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
        Form hi = new Form("Hi World", BoxLayout.y());
        Button leadBtn = new Button(">");
        Container cnt = new Container();
        Label label = new Label("Label");
        cnt.setLayout(new BorderLayout());
        cnt.add(BorderLayout.CENTER, label).add(BorderLayout.EAST, leadBtn);
        cnt.setLeadComponent(leadBtn);
        hi.add(cnt);
        
        Button justButton = new Button("Just Button");
        hi.add(justButton);
        
        /*
        Add the listeners. All pointer events on the Container cnt should
        report that the component is the leadBtn.
        Events on the justButton should report that they were from the justButton
        component.
        */
        $(cnt, leadBtn, label, justButton)
                .addActionListener(e->{
                    System.out.println("Action Event received by "+$(e).asComponent());
                })
                .addPointerPressedListener(e->{
                    System.out.println("Pointer press received by "+$(e).asComponent());
                })
                .addLongPressListener(e->{
                    System.out.println("Long press received by "+$(e).asComponent());
                    Component c = $(e).asComponent();
                    if (c instanceof Button) {
                        Button b = (Button)c;
                        b.released();
                    }
                })
                .addPointerReleasedListener(e->{
                    System.out.println("Pointer release received by "+$(e).asComponent());
                })
                ;
        
        
        
        
        hi.add(new Label("Hi World"));
        hi.show();
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
