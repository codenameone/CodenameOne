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
import com.codename1.annotations.*;
import com.codename1.binding.BindAttr;
import com.codename1.properties.Property;

class AnnotationComponentBindingJava001Snippet {

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
    // tag::annotation-component-binding-java-001[]
    @Bindable
    public class LoginModel {

        @Bind(name = "userField", attr = BindAttr.TEXT)
        private String user;
        public String getUser()              { return user; }
        public void   setUser(String u)      { this.user = u; }                  //

        @Bind(name = "rememberMe", attr = BindAttr.SELECTED)
        public boolean remember;                                                   //

        @Bind(name = "banner", attr = BindAttr.UIID, twoWay = false)
        public String bannerStyle;

        @Bind(name = "fullName",
              attr = BindAttr.TEXT,
              getter = "computeFullName",
              setter = "applyFullName")                                           //
        private String fullName;
        public String computeFullName()      { return fullName.toUpperCase(); }
        public void   applyFullName(String f){ this.fullName = f.trim(); }
    }
    // end::annotation-component-binding-java-001[]
}
