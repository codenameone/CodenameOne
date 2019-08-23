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
import com.codename1.ui.CN;
import com.codename1.ui.layouts.BorderLayout;

/**
 * This sample shows how to display a dialog that is laid out with its preferred size,
 * but so that the title bar of the form is still visible above and behind it.
 */
public class DialogPositioningSample {

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
        hi.add(new Label("Hi World"));
        Button btn = new Button("Show Dialog");
        btn.addActionListener(e->{
            Dialog dlg = new Dialog("Hello Dialog", new BorderLayout());
            dlg.add(BorderLayout.CENTER, BoxLayout.encloseY(new Label("Here is some text"), new Label("And Some More"), new Button("Cancel")));
            int padding = 0;
            if (!CN.isTablet()) {
                // If it is a tablet, we just let the dialog keep its preferred size.
                // If it is a phone then we want the dialog to stretch to the edge of the screen.
                dlg.getContentPane().setPreferredW(hi.getWidth() - dlg.getStyle().getHorizontalPadding() - dlg.getContentPane().getStyle().getHorizontalMargins());
            }
            int w = dlg.getDialogPreferredSize().getWidth();
            int h = dlg.getDialogPreferredSize().getHeight();
            
            // Position the top so that it is just underneath the form's title area.
            int top = hi.getTitleArea().getAbsoluteY() 
                    + hi.getTitleArea().getHeight() 
                    + hi.getTitleArea().getStyle().getMarginBottom() 
                    + padding;
            int left = (hi.getWidth() - w)/2;
            int right = left;
            int bottom = hi.getHeight() - top - h;
            System.out.println("bottom="+bottom);
            bottom = Math.max(0, bottom);
            top = Math.max(0, top);
            dlg.show(top, bottom, left, right);
        });
        hi.add(btn);
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
