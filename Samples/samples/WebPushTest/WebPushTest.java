package com.codename1.samples;


import com.codename1.io.ConnectionRequest;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.io.Log;
import com.codename1.io.NetworkManager;
import com.codename1.io.Preferences;
import com.codename1.push.Push;
import com.codename1.push.PushAction;
import com.codename1.push.PushActionCategory;
import com.codename1.push.PushActionsProvider;
import com.codename1.push.PushCallback;
import com.codename1.push.PushContent;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import java.io.IOException;
import java.util.Hashtable;


/**
 * This can be used to test push notifications in Codename One.
 * 
 * <p>It does require some setup to work on devices.</p>
 * 
 * <h3> Configuring Push in Samples</h3>

<h4>Android</h4>
<code><pre>
1. Goto https://console.firebase.google.com/ 
1.1 Add Project
1.2. Setup Cloud Messaging
1.3 Download google-services.json
1.4 Find FCM_SERVER_API_KEY from Project Settings &raquo; Cloud Messaging, and copy this for later use.

2. Copy google-services.json to config/WebPushTest/google-services.json
3. Launch sample in simulator.
4. Enter the FCM_SERVER_API_KEY into the the field provided in the form
</pre></code>
 */
public class WebPushTest implements PushCallback, PushActionsProvider {
    public static final String TOKEN="xxx";
    public static final String IOS_DEV_CERT_URL="https://codename-one-push-certificates.s3.amazonaws.com/xxxxxxxx.p12";
    public static final String IOS_PROD_CERT_URL="https://codename-one-push-certificates.s3.amazonaws.com/xxxxxxx.p12";
    public static final String IOS_DEV_CERT_PASS="xxxxxx";
    public static final String IOS_PROD_CERT_PASS="xxxxxx";
    
    private Form current;
    private Resources theme;

    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");

        // Enable Toolbar on all Forms by default
        Toolbar.setGlobalToolbar(true);

        // Pro only feature, uncomment if you have a pro subscription
        // Log.bindCrashProtection(true);
    }
    
    public void start() {
        if(current != null){
            current.show();
            return;
        }
        //Form hi = new Form("Hi World");
        //hi.addComponent(new Label("Hi World"));
        Display.getInstance().registerPush(new Hashtable(), true);
        //hi.show();
        createPushForm();
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
        if(current instanceof Dialog) {
            ((Dialog)current).dispose();
            current = Display.getInstance().getCurrent();
        }
    }
    
    public void destroy() {
    }

    @Override
    public void push(String value) {
        PushContent data = PushContent.get();
        if (data != null) {
            System.out.println("Image URL: "+data.getImageUrl());
            System.out.println("Category: "+data.getCategory());
            System.out.println("Action: "+data.getActionId());
            System.out.println("Text Response: "+data.getTextResponse());
        } else {
            System.out.println("PushContent is null");
        }
        System.out.println("Push "+value);
        Display.getInstance().callSerially(()->{
            Dialog.show("Push received", value, "OK", null);
        });
    }

    @Override
    public void registeredForPush(String deviceId) {
        System.out.println("Registered for push "+deviceId);
    }

    @Override
    public void pushRegistrationError(String error, int errorCode) {
        System.out.println("Push registration error "+error);
    }
    
    void createPushForm() {
        Form f = new Form("Test Push", new BorderLayout());
        TextField serverUrl = new TextField("https://push.codenameone.com/push/push");
        String nexus5XEmulatorId = "cn1-fcm-c_t22hK-weg:APA91bGNa2hP9SHLBhUmLmzY1ANyjCH2ydEQz0JUFlMPFUHDAUfTQysC1r9K6I7jdk5LN6mEP5k5IWvyQX4dvubbHeY1KWQRKb-aYuXaHHHg2U3XGH1xXXa7xfJbsCf7nB0vu1aWhOg_4M4Z-znC2X5a0tpYf59QmA";
                                  //APA91bHWiuoPMA4OgIr3ZyVgSjVmnh4H0BQ4jhB3hblIAZfmMs-SfRN1tb4662MudPEULjIkl8P_oTrQ14sKgowz4Q45n6iaPl1GwXb_9HbtlAQDAlnX60Eo4SamzZJkB_6kcnsEMKt_
        String iphoneId = "cn1-ios-c76b23de81f6389f37d2621f000b7f01c52e5c1d6f1f947b199a7c2d8844f713";
        
        
        TextArea targetId = new TextArea(Preferences.get("targetId", ""));
        targetId.addActionListener(e->Preferences.set("targetId", targetId.getText()));
        TextField pushType = new TextField(Preferences.get("pushType", "99"));
        pushType.addActionListener(e->Preferences.set("pushType", pushType.getText()));
        TextField payload = new TextField(Preferences.get("payload", "<push type=\"0\" body=\"Hello\" category=\"fo\"/>"));
        payload.addActionListener(e->Preferences.set("payload", payload.getText()));
        TextField gcmServerKey = new TextField(Preferences.get("gcmServerKey", ""));
        gcmServerKey.setHint("FCM_SERVER_API_KEY");
        gcmServerKey.addActionListener(e->Preferences.set("gcmServerKey", gcmServerKey.getText()));
        
        TextField tokenField = new TextField(Preferences.get("token", ""));
        tokenField.setHint("Enter your CN1 Token");
        tokenField.addActionListener(e->{
            Preferences.set("token", tokenField.getText());
        });
        
        targetId.setRows(4);
        targetId.getAllStyles().setFgColor(0x0);
        targetId.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        Button send = new Button("Send");
        send.addActionListener(e->{
            /*
            boolean result = new Push("E729F5CE-DCE8-4D63-B517-85112994EC66", payload.getText(), targetId.getText())
                    .gcmAuth(gcmServerKey.getText())
                    .apnsAuth(IOS_DEV_CERT_URL, IOS_DEV_CERT_PASS, false)
                    .pushType(Integer.parseInt(pushType.getText()))
                    .wnsAuth("xxxxx", "xxxxxxxx")
                    .send()
                    //.sendAsync()
                    ;
            
            System.out.println("Push sent.  Result: "+result);
            */
            ConnectionRequest req = new ConnectionRequest();
            req.setUrl(serverUrl.getText());
            req.addArgument("token", tokenField.getText());
            req.addArgument("device", targetId.getText());
            req.addArgument("body", payload.getText());
            req.addArgument("type", pushType.getText());
            req.addArgument("auth", gcmServerKey.getText());
            req.addArgument("cert", IOS_DEV_CERT_URL);
            req.addArgument("certPassword", IOS_DEV_CERT_PASS);
            req.addArgument("sid", "ms-app://xxxxxxxxx");
            req.addArgument("client_secret", "xxxxxxxxx");
            req.setHttpMethod("POST");
            req.setPost(true);
            NetworkManager.getInstance().addToQueueAndWait(req);
            System.out.println(req.getResponseCode());
            try {
                System.out.println(new String(req.getResponseData(), "UTF-8"));
            } catch (Throwable t) {Log.e(t);}
                   
            
            //new Push("E729F5CE-DCE8-4D63-B517-85112994EC66", payload.getText(), targetId.getText())
            //        .wnsAuth("ms-app://s-1-15-2-2674027049-292503787-1918612089-438606370-903203898-836476968-4131729547", "2S37cRtqCR3vQVqhfrFBA2w6PAsWwZ/m")
            //        .send();
            
        });
        
        Container center = BoxLayout.encloseY(new Label("Server URL"), serverUrl,
                new Label("Target ID"), targetId, new Label("Push Type"), pushType, new Label("Payload"), payload, new Label("GCM Server Key"), gcmServerKey, new Label("Codename One Token"), tokenField, send);
        center.setScrollableY(true);
        f.add(BorderLayout.CENTER, center);
        f.show();
        
    }

    @Override
    public PushActionCategory[] getPushActionCategories() {
        return new PushActionCategory[]{
            new PushActionCategory("fo", new PushAction[]{
                new PushAction("yes", "Yes"),
                new PushAction("no", "No"),
                new PushAction("maybe", "Maybe", null, "Enter reason", "Reply")
            })
            
        };
    }

}