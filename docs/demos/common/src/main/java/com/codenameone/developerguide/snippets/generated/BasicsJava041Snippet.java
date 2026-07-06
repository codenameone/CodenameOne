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


class BasicsJava041Snippet {

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
    // tag::basics-java-041[]
    /**
     * GUI builder created Form
     *
     * @author shai
     */
    public class MyForm extends com.codename1.ui.Form {

        public MyForm() {
            this(com.codename1.ui.util.Resources.getGlobalResources());
        }

        public MyForm(com.codename1.ui.util.Resources resourceObjectInstance) {
            initGuiBuilderComponents(resourceObjectInstance);
        }

    //-- DON'T EDIT BELOW THIS LINE!!!
        private com.codename1.ui.Label gui_Label_1 = new com.codename1.ui.Label();
        private com.codename1.ui.Button gui_Button_1 = new com.codename1.ui.Button();


    // <editor-fold defaultstate="collapsed" desc="Generated Code">
        private void guiBuilderBindComponentListeners() {
            EventCallbackClass callback = new EventCallbackClass();
            gui_Button_1.addActionListener(callback);
        }

        class EventCallbackClass implements com.codename1.ui.events.ActionListener, com.codename1.ui.events.DataChangedListener {
            private com.codename1.ui.Component cmp;
            public EventCallbackClass(com.codename1.ui.Component cmp) {
                this.cmp = cmp;
            }

            public EventCallbackClass() {
            }

            public void actionPerformed(com.codename1.ui.events.ActionEvent ev) {
                com.codename1.ui.Component sourceComponent = ev.getComponent();
                if(sourceComponent.getParent().getLeadParent() != null) {
                    sourceComponent = sourceComponent.getParent().getLeadParent();
                }

                if(sourceComponent == gui_Button_1) {
                    onButton_1ActionEvent(ev);
                }
            }

            public void dataChanged(int type, int index) {
            }
        }
        private void initGuiBuilderComponents(com.codename1.ui.util.Resources resourceObjectInstance) {
            guiBuilderBindComponentListeners();
            setLayout(new com.codename1.ui.layouts.FlowLayout());
            setTitle("My new title");
            setName("MyForm");
            addComponent(gui_Label_1);
            addComponent(gui_Button_1);
            gui_Label_1.setText("Hi World");
            gui_Label_1.setName("Label_1");
            gui_Button_1.setText("Click Me");
            gui_Button_1.setName("Button_1");
        }// </editor-fold>

    //-- DON'T EDIT ABOVE THIS LINE!!!
        public void onButton_1ActionEvent(com.codename1.ui.events.ActionEvent ev) {
        }

    }
    // end::basics-java-041[]
}
