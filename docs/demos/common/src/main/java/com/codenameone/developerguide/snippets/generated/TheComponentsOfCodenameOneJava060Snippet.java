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


class TheComponentsOfCodenameOneJava060Snippet {

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
com.codename1.ui.List list = new com.codename1.ui.List(createGenericListCellRendererModelData());


private Container createGenericRendererContainer() {
    Label name = new Label();
    name.setFocusable(true);
    name.setName("Name");
    Label surname = new Label();
    surname.setFocusable(true);
    surname.setName("Surname");
    CheckBox selected = new CheckBox();
    selected.setName("Selected");
    selected.setFocusable(true);
    Container c = BorderLayout.center(name).
            add(BorderLayout.SOUTH, surname).
            add(BorderLayout.WEST, selected);
    c.setUIID("ListRenderer");
    return c;
}

private Object[] createGenericListCellRendererModelData() {
    Map<String,Object>[] data = new HashMap[5];
    data[0] = new HashMap<>();
    data[0].put("Name", "Shai");
    data[0].put("Surname", "Almog");
    data[0].put("Selected", Boolean.TRUE);
    data[1] = new HashMap<>();
    data[1].put("Name", "Chen");
    data[1].put("Surname", "Fishbein");
    data[1].put("Selected", Boolean.TRUE);
    data[2] = new HashMap<>();
    data[2].put("Name", "Ofir");
    data[2].put("Surname", "Leitner");
    data[3] = new HashMap<>();
    data[3].put("Name", "Yaniv");
    data[3].put("Surname", "Vakarat");
    data[4] = new HashMap<>();
    data[4].put("Name", "Meirav");
    data[4].put("Surname", "Nachmanovitch");
    return data;
}
    void snippet() throws Exception {
        // tag::the-components-of-codename-one-java-060[]
        list.setRenderer(new GenericListCellRenderer(createGenericRendererContainer(), createGenericRendererContainer()));
        // end::the-components-of-codename-one-java-060[]
    }
}
