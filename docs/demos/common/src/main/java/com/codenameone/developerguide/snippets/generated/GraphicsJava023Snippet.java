package com.codenameone.developerguide.snippets.generated;

import com.codename1.gpu.*;
import com.codename1.ui.*;
import com.codename1.ui.animations.*;
import com.codename1.ui.events.*;
import com.codename1.ui.geom.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.list.*;
import com.codename1.ui.plaf.*;
import com.codename1.ui.util.*;
import com.codename1.components.*;
import com.codename1.charts.models.*;
import com.codename1.charts.renderers.*;
import com.codename1.charts.views.*;
import com.codename1.capture.*;
import com.codename1.io.*;
import com.codename1.l10n.*;
import com.codename1.location.*;
import com.codename1.maps.*;
import com.codename1.media.*;
import com.codename1.messaging.*;
import com.codename1.payment.*;
import com.codename1.processing.*;
import com.codename1.properties.*;
import com.codename1.push.*;
import com.codename1.security.*;
import com.codename1.social.*;
import com.codename1.ui.spinner.*;
import java.io.*;
import java.util.*;


class GraphicsJava023Snippet {

    Object context;
    Object url;
    Object value;
    Object body;
    Object event;
    String apiKey = "test-key";
    String myHttpsURL = "https://example.com";
    java.util.List<String> validKeysList = new java.util.ArrayList<>();
    Image myImage;
    Graphics graphics;
    Graphics g;
    GraphicsDevice device;
    Form form;
    Form hi;
    Container cnt;
    Container myForm;
    Component component;
    Button button;
    MultiButton myMultiButton;
    Label label;
    BrowserComponent browserComponent;
    Resources theme;
    void snippet() throws Exception {
        // tag::graphics-java-023[]
        Image duke = null;
        try {
            // duke.png is just the default Codename One icon copied into place
            duke = Image.createImage("/duke.png");
        } catch(IOException err) {
            Log.e(err);
        }
        final Image finalDuke = duke;

        Form hi = new Form("Shape Clip");

        // We create a 50 x 100 shape, this is arbitrary since we can scale it easily
        GeneralPath path = new GeneralPath();
        path.moveTo(20,0);
        path.lineTo(30, 0);
        path.lineTo(30, 100);
        path.lineTo(20, 100);
        path.lineTo(20, 15);
        path.lineTo(5, 40);
        path.lineTo(5, 25);
        path.lineTo(20,0);

        Stroke stroke = new Stroke(0.5f, Stroke.CAP_ROUND, Stroke.JOIN_ROUND, 4);
        hi.getContentPane().getUnselectedStyle().setBgPainter((Graphics g, Rectangle rect) -> {
            g.setColor(0xff);
            float widthRatio = ((float)rect.getWidth()) / 50f;
            float heightRatio = ((float)rect.getHeight()) / 100f;
            g.scale(widthRatio, heightRatio);
            g.translate((int)(((float)rect.getX()) / widthRatio), (int)(((float)rect.getY()) / heightRatio));
            g.setClip(path);
            g.setAntiAliased(true);
            g.drawImage(finalDuke, 0, 0, 50, 100);
            g.setClip(path.getBounds());
            g.drawShape(path, stroke);
            g.translate(-(int)(((float)rect.getX()) / widthRatio), -(int)(((float)rect.getY()) / heightRatio));
            g.resetAffine();
        });

        hi.show();
        // end::graphics-java-023[]
    }
}
