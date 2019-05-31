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
import com.codename1.ui.Graphics;
import com.codename1.ui.Painter;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.Rectangle;

/**
 * A sample of subtracting a circle from inside a rectangle using the Shape API.
 */
public class ShapeDonutSample {

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
        hi.getContentPane().getStyle().setBgPainter(new Painter() {
            @Override
            public void paint(Graphics g, Rectangle rect) {
                GeneralPath p = new GeneralPath();
                
                p.setRect(new Rectangle(rect.getX()+10, rect.getY()+10, rect.getWidth()-20, rect.getHeight()-20), null);
                
                // Since default winding rule is EVEN_ODD, when we fill a shape with a closed circle inside a
                // rectangle, it will result in the circle being subtracted from the rect.
                p.arc(rect.getX()+30, rect.getY()+30, rect.getWidth()-60, rect.getHeight()-60, 0, Math.PI*2);
                g.setColor(0x0000ff);
                
                
                g.fillShape(p);
            }
            
        });
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
