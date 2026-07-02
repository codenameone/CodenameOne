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


class IoJava032Snippet {

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
        // tag::io-java-032[]
        Form hi = new Form("JSON Parsing", new BoxLayout(BoxLayout.Y_AXIS));
        JSONParser json = new JSONParser();
        try(Reader r = new InputStreamReader(Display.getInstance().getResourceAsStream(getClass(), "/anapioficeandfire.json"), "UTF-8");) {
            Map<String, Object> data = json.parseJSON(r);
            java.util.List<Map<String, Object>> content = (java.util.List<Map<String, Object>>)data.get("root"); //
            for(Map<String, Object> obj : content) { //
                String url = (String)obj.get("url");
                String name = (String)obj.get("name");
                java.util.List<String> titles =  (java.util.List<String>)obj.get("titles"); //
                if(name == null || name.length() == 0) {
                    java.util.List<String> aliases = (java.util.List<String>)obj.get("aliases");
                    if(aliases != null && aliases.size() > 0) {
                        name = aliases.get(0);
                    }
                }
                MultiButton mb = new MultiButton(name);
                if(titles != null && titles.size() > 0) {
                    mb.setTextLine2(titles.get(0));
                }
                mb.addActionListener((e) -> Display.getInstance().execute(url));
                hi.add(mb);
            }
        } catch(IOException err) {
            Log.e(err);
        }
        hi.show();
        // end::io-java-032[]
    }
}
