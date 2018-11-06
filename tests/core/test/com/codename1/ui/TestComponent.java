/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.ui;

import com.codename1.components.InteractionDialog;
import com.codename1.components.ToastBar;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.Cookie;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.maps.Coord;
import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Point;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Style;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.Map;

/**
 *
 * @author shannah
 */
public class TestComponent extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {

        //testIOSThreadsError35();
        getComponentAt_int_int();
        List_shouldRenderSelection();
        testSimpleDateFormat();
        testCookies();
        testCookiesInBrowserComponent();
        
        testBrowserComponent2267();
        findCommandComponent();
        testOverflowMenuNPE();
        /* Temporarily disabling these tests as they currently fail and it isn't
           clear what the best way is to make them pass since it uses deprecated APIs

        //testSideMenuCloseNPE(false, false);
        testSideMenuCloseNPE(false, true);
        //testSideMenuCloseNPE(false, true);
        //testSideMenuCloseNPE(true, false);
        testSideMenuCloseNPE(true, true);
        */
        runOwnerTests();
        return true;
    }

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }


    private void getComponentAt_int_int() {
        testNestedScrollingLabels();
        getComponentAt_int_int_nested();
        getComponentAt_int_int_button();
        getComponentAt_int_int_label();
        getComponentAt_int_int_container();
        getComponentAt_int_int_browsercomponent();

    }

    private void List_shouldRenderSelection() {
        com.codename1.ui.List l = new com.codename1.ui.List<>();

        // Make sure this doesn't throw a stack overflow error.
        try {
            l.shouldRenderSelection();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException("Error in List.shouldRenderSelection");
        }
    }




    private void getComponentAt_int_int_button() {
        log("Testing getComponentAt(x, y) with button");
        int w = Display.getInstance().getDisplayWidth();
        int h = Display.getInstance().getDisplayHeight();

        Form f = new Form("My Form", new BorderLayout());
        Label l = new Button("Hello");
        f.add(BorderLayout.CENTER, l);

        f.show();
        TestUtils.waitForFormTitle("My Form");
        Component middleComponent = f.getComponentAt(w/2, h/2);
        assertEqual(l, middleComponent, "Found wrong component");
    }

    private void getComponentAt_int_int_label() {
        log("Testing getComponentAt(x, y) with label");
        int w = Display.getInstance().getDisplayWidth();
        int h = Display.getInstance().getDisplayHeight();

        Form f = new Form("My Form", new BorderLayout());
        Label l = new Label("Hello");
        f.add(BorderLayout.CENTER, l);

        f.show();
        TestUtils.waitForFormTitle("My Form", 2000);
        Component middleComponent = f.getComponentAt(w/2, h/2);
        assertEqual(l, middleComponent, "Found wrong component");


    }
    
    
    private void getComponentAt_int_int_nested() {
        log("Testing getComponentAt(x, y) with layered nesting");
        int w = Display.getInstance().getDisplayWidth();
        int h = Display.getInstance().getDisplayHeight();

        Form f = new Form("My Form", new BorderLayout());
        Container bottom = new Container() {

            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(w, h);
            }
            
        };
        bottom.setGrabsPointerEvents(true);
        bottom.setName("Bottom");
        
        Container top = new Container(BoxLayout.y());
        //top.setScrollableY(true);
        top.setScrollableY(true);
        top.setGrabsPointerEvents(true);
        
        Component content = new Component() {

            @Override
            protected Dimension calcPreferredSize() {
                return new Dimension(w, h * 2);
            }
            
        };
        content.setFocusable(false);
        content.setGrabsPointerEvents(false);
        content.setName("Content");
        top.add(content);
        top.setName("Top");
        Container wrapper = new Container(new LayeredLayout());
        wrapper.add(bottom).add(top);
        
        
        
        f.add(BorderLayout.CENTER, wrapper);

        f.show();
        TestUtils.waitForFormTitle("My Form", 2000);
        Component middleComponent = f.getComponentAt(w/2, h/2);
        assertEqual(content, middleComponent, "Found wrong component");
    }

    private void testNestedScrollingLabels() {
        int w = Display.getInstance().getDisplayWidth();
        int h = Display.getInstance().getDisplayHeight();
        Form f = new Form("Scrolling Labels");
        Toolbar tb = new Toolbar();
        f.setToolbar(tb);
        final Form backForm = Display.getInstance().getCurrent();
        tb.addCommandToSideMenu(new Command("Back") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (backForm != null) {
                    backForm.showBack();
                }
            }
        });
        f.setTitle("Scrolling Labels");
        Container cnt = new Container(BoxLayout.y());
        cnt.setScrollableY(true);
        for (String l : new String[]{"Red", "Green", "Blue", "Orange", "Indigo"}) {
            for (int i=0; i<20; i++) {
                cnt.add(l+i);
            }
        }

        f.setLayout(new BorderLayout());
        f.add(BorderLayout.CENTER, LayeredLayout.encloseIn(new Button("Press me"), BorderLayout.center(BorderLayout.center(cnt))));
        f.show();

        TestUtils.waitForFormTitle("Scrolling Labels", 2000);
        Component res = f.getComponentAt(w/2, h/2);
        assertTrue(res == cnt || res.getParent() == cnt, "getComponentAt(x,y) should return scrollable container on top of button when in layered pane.");

    }

    private void getComponentAt_int_int_container() {
        int w = Display.getInstance().getDisplayWidth();
        int h = Display.getInstance().getDisplayHeight();

        Form f = new Form("My Form", new BorderLayout());
        Container l = new Container();
        f.add(BorderLayout.CENTER, l);

        f.show();
        TestUtils.waitForFormTitle("My Form", 2000);
        Component middleComponent = f.getComponentAt(w/2, h/2);
        assertEqual(l, middleComponent, "Found wrong component");
    }

    private void getComponentAt_int_int_browsercomponent() {
        int w = Display.getInstance().getDisplayWidth();
        int h = Display.getInstance().getDisplayHeight();
        Form mapDemo = new Form("Maps", new LayeredLayout());
        Toolbar.setOnTopSideMenu(true);
        Toolbar tb = new Toolbar();
        mapDemo.setToolbar(tb);
        mapDemo.setTitle("Maps");
        tb.addCommandToSideMenu(new Command("Test") {
            @Override
            public void actionPerformed(ActionEvent e) {
                //testNestedScrollingLabels();
            }
        });
        BrowserComponent mc = new BrowserComponent();
        mapDemo.add(mc);

        mapDemo.show();

        TestUtils.waitForFormTitle("Maps", 2000);
        Component middleComponent = mapDemo.getComponentAt(w/2, h/2);
        assertTrue(mc == middleComponent || mc.contains(middleComponent),  "Wrong component found in middle. Expected "+mc+" but found "+middleComponent);

        tb.openSideMenu();
        TestUtils.waitFor(500); // wait for side menu to open

        Component res = null;

        res = tb.getComponentAt(10, h/2);

        //System.out.println("tb size = "+tb.getAbsoluteX()+", "+tb.getAbsoluteY()+", "+tb.getWidth()+", "+tb.getHeight());
        //System.out.println("mb size = "+tb.getMenuBar().getAbsoluteX()+", "+tb.getMenuBar().getAbsoluteY()+", "+tb.getMenuBar().getWidth()+", "+tb.getMenuBar().getHeight());
        //System.out.println("res is "+res);
        res = mapDemo.getComponentAt(10, h/2);

        // Let's find the interaction dialog on the form
        Component interactionDialog = $("*", mapDemo).filter(c->{
            return c instanceof InteractionDialog;
        }).asComponent();

        assertTrue(((InteractionDialog)interactionDialog).contains(res), "Toolbar is open so getComponentAt() should return something on the toolbar.  But received "+res+".  Toolbar is "+tb);

    }

    private void testCookies() throws IOException {
        Cookie.clearCookiesFromStorage();
        String baseUrl = "http://solutions.weblite.ca/cn1tests/cookie";
        String clearCookiesUrl = baseUrl +"/reset.php";
        String setCookiesUrl = baseUrl + "/set.php";
        String checkCookiesUrl = baseUrl + "/check.php";
        String setCookiesUrlSession = baseUrl + "/set_session.php";

        // Try without native cookie store
        ConnectionRequest.setUseNativeCookieStore(false);
        ConnectionRequest.fetchJSON(clearCookiesUrl);
        Map<String,Object> res = ConnectionRequest.fetchJSON(checkCookiesUrl);
        System.out.println(res);
        TestUtils.assertBool(null == res.get("cookieval"), "Cookie should be null after clearing cookies but was "+res.get("cookieval"));
        ConnectionRequest.fetchJSON(setCookiesUrl);
        res = ConnectionRequest.fetchJSON(checkCookiesUrl);
        TestUtils.assertEqual("hello", res.get("cookieval"), "Cookie set to incorrect value.");

        // Now check that session cookies (no explicit expiry) are set correctly
        ConnectionRequest.fetchJSON(clearCookiesUrl);
        res = ConnectionRequest.fetchJSON(checkCookiesUrl);
        TestUtils.assertBool(null == res.get("cookieval"), "Cookie should be null after clearing cookies but was "+res.get("cookieval"));
        ConnectionRequest.fetchJSON(setCookiesUrlSession);
        res = ConnectionRequest.fetchJSON(checkCookiesUrl);
        TestUtils.assertEqual("hello", res.get("cookieval"), "Cookie set to incorrect value.");

        // Try with native cookie store
        ConnectionRequest.setUseNativeCookieStore(true);
        ConnectionRequest.fetchJSON(clearCookiesUrl);
        res = ConnectionRequest.fetchJSON(checkCookiesUrl);
        TestUtils.assertBool(null == res.get("cookieval"), "Cookie should be null after clearing cookies but was "+res.get("cookieval"));
        ConnectionRequest.fetchJSON(setCookiesUrl);
        res = ConnectionRequest.fetchJSON(checkCookiesUrl);
        TestUtils.assertEqual("hello", res.get("cookieval"), "Cookie set to incorrect value.");

        // Now check that session cookies (no explicit expiry) are set correctly
        ConnectionRequest.fetchJSON(clearCookiesUrl);
        res = ConnectionRequest.fetchJSON(checkCookiesUrl);
        TestUtils.assertBool(null == res.get("cookieval"), "Cookie should be null after clearing cookies but was "+res.get("cookieval"));
        ConnectionRequest.fetchJSON(setCookiesUrlSession);
        res = ConnectionRequest.fetchJSON(checkCookiesUrl);
        TestUtils.assertEqual("hello", res.get("cookieval"), "Cookie set to incorrect value.");
        
        Throwable[] t = new Throwable[1];
        // Now test a different cookie date format.
        ConnectionRequest req = new ConnectionRequest() {

            @Override
            protected void handleException(Exception err) {
                Log.p("handling exception "+err);
                t[0] = err;
            }

            @Override
            protected void handleRuntimeException(RuntimeException err) {
                Log.p("handling runtime exception "+err);
                t[0] = err;
            }

            @Override
            protected void handleErrorResponseCode(int code, String message) {
                Log.p("Error response "+code+", "+message);
            }
            
            
            
        };
        
       
        String oldProp = (String)Display.getInstance().getProperty("com.codename1.io.ConnectionRequest.throwExceptionOnFailedCookieParse", null);
        Display.getInstance().setProperty("com.codename1.io.ConnectionRequest.throwExceptionOnFailedCookieParse", "true");
        req.setUrl(baseUrl + "/test_rfc822cookie.php");
        req.setFollowRedirects(true);
        req.setPost(false);
        req.setDuplicateSupported(true);
        
        //req.setFailSilently(true);
        try {
            NetworkManager.getInstance().addToQueueAndWait(req);
            
        } finally {
            //NetworkManager.getInstance().removeErrorListener(errorListener);
            Display.getInstance().setProperty("com.codename1.io.ConnectionRequest.throwExceptionOnFailedCookieParse", oldProp);
        }
        TestUtils.assertTrue(req.getResponseCode() == 200, "Unexpected response code.  Expected 200 but found "+req.getResponseCode());
        TestUtils.assertTrue(t[0] == null, t[0] != null ? ("Exception was thrown getting URL "+t[0].getMessage()):"");


    }

    private static class BrowserStatus {
        private static final int LOADING=0;
        private static final int READY=1;
        int status;
        String content;
        final BrowserComponent bc;

        BrowserStatus(BrowserComponent bc) {
            this.bc = bc;
            bc.addWebEventListener("onLoad", e->{
                ready(bc.executeAndReturnString("document.body.innerHTML"));
            });
        }

        synchronized void reset() {
            content = null;
            status = LOADING;
        }
        synchronized void ready(String content) {
            status = READY;
            this.content = content;
            notifyAll();
        }

        void waitReady() {
            while (status != READY) {
                Display.getInstance().invokeAndBlock(()->{
                    synchronized(BrowserStatus.this) {
                        Util.wait(BrowserStatus.this, 2000);
                    }
                });
            }
        }

        Map<String,Object> getJSONContent() throws IOException {
            JSONParser p = new JSONParser();
            String c = content.substring(content.indexOf("{"), content.lastIndexOf("}")+1);
            return p.parseJSON(new InputStreamReader(new ByteArrayInputStream(c.getBytes("UTF-8")), "UTF-8"));
        }
    }

    private void testCookiesInBrowserComponent() throws IOException {
        Cookie.clearCookiesFromStorage();
        Form f = new Form("CookiesInBrowser");
        String formName = "CookiesInBrowser";
        f.setName(formName);
        f.setLayout(new BorderLayout());
        BrowserComponent bc = new BrowserComponent();
        f.add(BorderLayout.CENTER, bc);
        f.show();
        TestUtils.waitForFormName(formName, 2000);
        String baseUrl = "http://solutions.weblite.ca/cn1tests/cookie";
        String clearCookiesUrl = baseUrl +"/reset.php";
        String setCookiesUrl = baseUrl + "/set.php";
        String checkCookiesUrl = baseUrl + "/check.php";
        String setCookiesUrlSession = baseUrl + "/set_session.php";



        final BrowserStatus status = new BrowserStatus(bc);
        bc.setURL(clearCookiesUrl);
        status.waitReady();
        status.reset();
        bc.setURL(checkCookiesUrl);
        status.waitReady();
        Map<String,Object> res = status.getJSONContent();
        TestUtils.assertBool(null == res.get("cookieval"), "Cookie should be null after clearing cookies but was "+res.get("cookieval"));
        status.reset();
        bc.setURL(setCookiesUrl);
        status.waitReady();
        status.reset();
        bc.setURL(checkCookiesUrl);
        status.waitReady();
        res = status.getJSONContent();

        TestUtils.assertEqual("hello", res.get("cookieval"), "Cookie set to incorrect value.");
        status.reset();
        bc.setURL(clearCookiesUrl);
        status.waitReady();
        status.reset();
        bc.setURL(checkCookiesUrl);
        status.waitReady();
        res = status.getJSONContent();
        TestUtils.assertBool(null == res.get("cookieval"), "Cookie should be null after clearing cookies but was "+res.get("cookieval"));
        status.reset();

        bc.setURL(setCookiesUrlSession);
        status.waitReady();

        status.reset();
        bc.setURL(checkCookiesUrl);
        status.waitReady();
        res = status.getJSONContent();
        TestUtils.assertEqual("hello", res.get("cookieval"), "Cookie set to incorrect value.");

        // Now let's try to share cookies between the browser component and
        // a connection request.

        ConnectionRequest.setUseNativeCookieStore(true);
        Cookie.clearCookiesFromStorage();

        BrowserComponent bc2 = new BrowserComponent();
        bc.getParent().replace(bc, bc2, null);
        bc = bc2;
        f.revalidate();

        final BrowserStatus status2 = new BrowserStatus(bc);
        bc.setURL(clearCookiesUrl);
        status2.waitReady();
        status2.reset();

        // First verify that the cookie is not set in either browser or connection request

        bc.setURL(checkCookiesUrl);
        status2.waitReady();
        res = status2.getJSONContent();
        TestUtils.assertBool(null == res.get("cookieval"), "Cookie should be null after clearing cookies but was "+res.get("cookieval"));


        res = ConnectionRequest.fetchJSON(checkCookiesUrl);
        TestUtils.assertBool(null == res.get("cookieval"), "Cookie should be null after clearing cookies but was "+res.get("cookieval"));

        // Next let's set the cookie in the browser, and verify that it is set in both
        // browser and connection request.
        status2.reset();
        bc.setURL(setCookiesUrl);
        status2.waitReady();

        status2.reset();
        bc.setURL(checkCookiesUrl);
        status2.waitReady();
        res = status2.getJSONContent();
        TestUtils.assertEqual("hello", res.get("cookieval"), "Cookie set to incorrect value.");

        res = ConnectionRequest.fetchJSON(checkCookiesUrl);
        TestUtils.assertEqual("hello", res.get("cookieval"), "Cookie set to incorrect value.");


        // Now let's delete the cookie in the browser and verify that it is deleted in
        // both the browser and connection request.
        status2.reset();
        bc.setURL(clearCookiesUrl);
        status2.waitReady();

        status2.reset();
        bc.setURL(checkCookiesUrl);
        status2.waitReady();
        res = status2.getJSONContent();
        TestUtils.assertBool(null == res.get("cookieval"), "Cookie should be null after clearing cookies but was "+res.get("cookieval"));

        res = ConnectionRequest.fetchJSON(checkCookiesUrl);
        TestUtils.assertBool(null == res.get("cookieval"), "Cookie should be null after clearing cookies but was "+res.get("cookieval"));

        // Now let's set the cookie in the ConnectionRequest and verify that it is set in both
        // connection request and browser.

        ConnectionRequest.fetchJSON(setCookiesUrl);
        res = ConnectionRequest.fetchJSON(checkCookiesUrl);
        TestUtils.assertEqual("hello", res.get("cookieval"), "Cookie set to incorrect value.");

        status2.reset();
        bc.setURL(checkCookiesUrl);
        status2.waitReady();
        res = status2.getJSONContent();
        TestUtils.assertEqual("hello", res.get("cookieval"), "Cookie set to incorrect value.");

    }
    // Test for https://github.com/codenameone/CodenameOne/issues/2267
    private void testBrowserComponent2267() {
        Form hi = new Form();
        String formName = "testBrowserComponent2267";
        hi.setName(formName);
        hi.setLayout(new BorderLayout());

        BrowserComponent browserComponent = new BrowserComponent();
        hi.add(BorderLayout.CENTER, browserComponent);

        final Throwable[] ex = new Throwable[1];
        final boolean[] complete = new boolean[1];
        Button loadButton = new Button("setUrl");
        String buttonName = "setUrl";
        loadButton.setName(buttonName);
        loadButton.addActionListener((ev) -> {
                ActionListener errorHandler = new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        e.consume();
                        ex[0] = (Throwable)e.getSource();
                        complete[0] = true;
                        Display.getInstance().removeEdtErrorHandler(this);
                    }
                };
                try {


                    Display.getInstance().addEdtErrorHandler(errorHandler);
                    browserComponent.addWebEventListener("onLoad", e-> {
                        Display.getInstance().removeEdtErrorHandler(errorHandler);
                        complete[0] = true;
                    });
                    browserComponent.setURL("https://www.google.es");
                } catch (Throwable t) {
                    ex[0] = t;
                    complete[0] = true;
                    Display.getInstance().removeEdtErrorHandler(errorHandler);
                } finally {
                    //complete[0] = true;
                }
        });
        hi.add(BorderLayout.SOUTH, loadButton);
        hi.show();
        TestUtils.waitForFormName(formName, 2000);
        TestUtils.clickButtonByName(buttonName);
        while (!complete[0]) {
            Display.getInstance().invokeAndBlock(()->{
                Util.sleep(50);
            });
        }
        String message = null;
        if (ex[0] != null) {
            message = ex[0].getMessage();
        }
        TestUtils.assertBool(ex[0] == null, "We received an exception setting the browserComponent URL: "+message);
    }


    // Test to reproduce NPE reported here:
    // https://stackoverflow.com/questions/47141271/codename1-nullpointerexception-after-update
    // Initial fix did not fix case where global toolbar is false.
    // https://github.com/codenameone/CodenameOne/commit/2c7ad31b9542dd8bbff2ab00c5a4b0728efa9d0c
    //
    private void testSideMenuCloseNPE(boolean globalToolbar, boolean onTopSideMenu) {
        System.out.println("Testing SideMenuNPE with globalToolbar="+globalToolbar+"; onTopSideMenu="+onTopSideMenu);
        boolean globalToolbarSetting = Toolbar.isGlobalToolbar();
        boolean onTopSideMenuSetting = Toolbar.isOnTopSideMenu();
        int commandBehaviour = Display.getInstance().getCommandBehavior();
        Toolbar.setGlobalToolbar(globalToolbar);

        Toolbar.setOnTopSideMenu(onTopSideMenu);
        Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_SIDE_NAVIGATION);
        try {

            Form reset = new Form("Reset");
            reset.setName("Reset");
            reset.show();
            TestUtils.waitForFormName("Reset");
            Form home = new Form("Home");
            home.setName("Home");
            Toolbar homeTb = new Toolbar();
            //if (!globalToolbar){
                home.setToolbar(homeTb);
                TestUtils.assertEqual(homeTb, home.getToolbar(), "getToolbar() returned wrong toolbar");
            //}

            Form form1 = new Form("Form1");
            form1.setName("Form1");
            form1.setToolbar(new Toolbar());


            //Add navigation commands to the home Form
            NavigationCommand homeCommand = new NavigationCommand("Home");
            homeCommand.setNextForm(home);
            home.getToolbar().addCommandToSideMenu(homeCommand);

            NavigationCommand cmd1 = new NavigationCommand("Form1");
            cmd1.setNextForm(form1);
            home.getToolbar().addCommandToSideMenu(cmd1);

            home.show();

            TestUtils.waitForFormName("Home");
            home.getToolbar().openSideMenu();
            TestUtils.waitFor(500); // wait for side menu to open

            Button cmd1Btn;

            if (Toolbar.isOnTopSideMenu()) {
                cmd1Btn = $("SideCommand", home)
                        .filter(c->{
                            return "Form1".equals(((Button)c).getText());
                        }).asComponent(Button.class);
            } else {
                System.out.println(home.getToolbar().getMenuBar());
                cmd1Btn = $("SideCommand", home.getToolbar().getMenuBar())
                        .filter(c->{
                            return "Form1".equals(((Button)c).getText());
                        })
                        .asComponent(Button.class);
            }

            Button c = cmd1Btn;
            System.out.println(c);
            int actualX = c.getAbsoluteX() + (int)(0.5f * c.getWidth());
            int actualY = c.getAbsoluteY() + (int)(0.5f * c.getHeight());
            Display.getInstance().getCurrent().pointerPressed(actualX, actualY);
            Display.getInstance().getCurrent().pointerReleased(actualX, actualY);
            TestUtils.waitForFormName("Form1");
            //home.getToolbar().getMenuBar().actionPerformed(new ActionEvent(home));



            //home.getToolbar().sho

        } finally {
            Toolbar.setGlobalToolbar(globalToolbarSetting);
            Display.getInstance().setCommandBehavior(commandBehaviour);
            Toolbar.setOnTopSideMenu(onTopSideMenuSetting);
        }
    }

    private void findCommandComponent() {
        Form f = new Form();
        Toolbar tb = new Toolbar();
        f.setToolbar(tb);
        Command cmd = tb.addMaterialCommandToLeftBar(null, FontImage.MATERIAL_3D_ROTATION, 4.5f, e-> {

        });

        Component res = tb.findCommandComponent(cmd);

        TestUtils.assertTrue(res != null, "Could not find command component added to left bar.");

    }

    /**
     * https://github.com/codenameone/CodenameOne/issues/2255
     */
    public void testOverflowMenuNPE() {
        Form hi = new Form();
        hi.setName("testOverflowMenuNPE");
        hi.getToolbar().addCommandToOverflowMenu("Test", FontImage.createMaterial(FontImage.MATERIAL_3D_ROTATION, new Style()), new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                       ToastBar.showMessage("menu", FontImage.MATERIAL_3D_ROTATION);
                       hi.revalidate();
                    }
                });
        hi.show();

        TestUtils.waitForFormName("testOverflowMenuNPE", 2000);
        hi.getToolbar().closeSideMenu();

    }
    
    public void testSimpleDateFormat() {
        
        try {
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z");
            format.parse("mon, 20-nov-2017 19:49:58 gmt");
            format = new SimpleDateFormat("EEE dd-MMM-yyyy HH:mm:ss z");
            format.parse("mon 20-nov-2017 19:49:58 gmt");
            
            format = new SimpleDateFormat("EEE dd-MMM-yyyy HH:mm:ss z");
            java.util.Date dt = format.parse("tue 21-nov-2017 17:04:27 gmt");
            if (Display.getInstance().isSimulator()) {
                java.text.SimpleDateFormat format0 = new java.text.SimpleDateFormat("EEE dd-MMM-yyyy HH:mm:ss z", java.util.Locale.US);
                java.util.Date dt0 = format0.parse("tue 21-nov-2017 17:04:27 gmt");
                TestUtils.assertEqual(dt0, dt, "SimpleDateFormat gives different result than java.text version on tue 21-nov-2017 17:04:27 gmt with format \"EEE, dd-MMM-yyyy HH:mm:ss z\"");
            }
            TestUtils.assertBool(dt.getTime() / 1000L == 1511283867L, "Incorrect date for simple date format parse");
            
            format = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z");
            com.codename1.l10n.ParseException pex = null;
            try {
                format.parse("tue 21-nov-2017 17:04:27 gmt");
            } catch (com.codename1.l10n.ParseException pex2)
            {
                pex = pex2;
            }
            TestUtils.assertTrue(pex!=null, "Parsing date with wrong format should give parse exception");
            
            format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
            dt = format.parse("sun, 22 nov 2037 13:20:46 -0000");
            //2142508846
            //Log.p("Difference = "+(dt.getTime() - 2142508846000L));
            TestUtils.assertEqual(dt.getTime()/1000L,  2142508846L, "Failed to parse RFC822 datetime.  "+dt);
            
            String dateStr = "sun 22 nov 2037 13:20:46 -0000";
            format = new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss Z");
            dt = format.parse("sun 22 nov 2037 13:20:46 -0000");
            
            TestUtils.assertEqual(dt.getTime()/1000L,  2142508846L, "Failed to parse RFC822 datetime no comma.  "+dt);
        } catch (Throwable t) {
            Log.e(t);
            throw new RuntimeException("Failed to parse date mon 20-nov-2017 19:49:58 gmt: "+t.getMessage());
            
        }
        
        
        
    }
    
    public void testIOSThreadsError35() {
        if ("ios".equals(Display.getInstance().getPlatformName()) && !Display.getInstance().isSimulator()) {
            
            // We get the number 11002 from http://www.scsc.no/blog/2007/11-15-thread-creation-using-pthreadcreate-on-leopard.html
            final int numThreads = 11002;
            int blockSize = 10;
            int pos = 0;
            while (pos < numThreads) {
                
                Log.p("Creating threads "+pos+" to "+(pos+blockSize)+"...");
                final int[] remainingThreads = new int[]{blockSize};
                for (int i=0; i<blockSize; i++) {
                    new Thread(new Runnable() {
                        public void run() {
                            synchronized (remainingThreads) {
                                remainingThreads[0]--;
                            }
                        }
                    }).start();
                }
                while (remainingThreads[0] > 0) {
                    Display.getInstance().invokeAndBlock(()->{
                        Util.sleep(100);
                    });
                }
                pos += blockSize;
                Log.p("Finished with threads "+(pos-blockSize)+" to "+pos);
            }
        }
    }
    
    Point c(Component cmp) {
            return new Point(cmp.getAbsoluteX()+cmp.getScrollX()+cmp.getWidth()/2, cmp.getAbsoluteY()+cmp.getScrollY()+cmp.getHeight()/2);
        }
        
        boolean runOwnerTests() throws Exception {
            Container cnt = new Container();
            Button b2 = new Button();
            assertTrue(!cnt.isOwnedBy(b2), "cnt is not owned by b2");
            assertTrue(!b2.isOwnedBy(cnt), "b2 is not owned by cn1");
            cnt.add(b2);
            assertTrue(!b2.isOwnedBy(cnt), "button should not be owned by cn1 even though it is a child");
            b2.setOwner(cnt);
            assertTrue(b2.isOwnedBy(cnt), "Button should be owned by container");
            b2.setOwner(null);
            assertTrue(!b2.isOwnedBy(cnt), "Button should no longer be owned by container");

            Container c2 = new Container();
            c2.setOwner(cnt);
            assertTrue(c2.isOwnedBy(cnt), "c2 should be owned by cnt");
            b2.remove();
            c2.add(b2);
            assertTrue(b2.isOwnedBy(cnt), "b2 should be owned by cnt because its parent is owned by cnt");
            b2.remove();
            assertTrue(!b2.isOwnedBy(cnt), "b2 should no longer be owned by cnt");

            b2.setOwner(c2);
            assertTrue(b2.isOwnedBy(cnt), "b2 should be owned by cnt because of the owner hierarchy through c2");

            Form f1 = new Form("Test Form", BoxLayout.y());
            f1.setName("TestForm");
            f1.show();
            waitForFormName("TestForm");
            //waitFor(2000);
            Button b3 = new Button("B3");
            Button b4 = new Button("B4");
            f1.add(b3).add(b4);
            f1.revalidate();
            //System.out.println(c(b3));
            //System.out.println(b3.contains(c(b3).getX(), c(b3).getY()));
            //System.out.println("Absx="+b3.getAbsoluteX()+", absy="+b3.getAbsoluteY()+", w="+b3.getWidth()+", h="+b3.getHeight());
            assertTrue(b3.contains(c(b3).getX(), c(b3).getY()));
            assertTrue(!b3.contains(c(b4).getX(), c(b4).getY()));
            assertTrue(b3.containsOrOwns(c(b3).getX(), c(b3).getY()));
            assertTrue(!b3.containsOrOwns(c(b4).getX(), c(b4).getY()));
            
            b4.setOwner(b3);
            assertTrue(b3.containsOrOwns(c(b4).getX(), c(b4).getY()));
            
            Container c4 = new Container(BoxLayout.x());
            Button b5 = new Button("B5");
            c4.add(b5);
            f1.add(c4);
            f1.revalidate();
            assertTrue(!b3.containsOrOwns(c(b5).getX(), c(b5).getY()));
            c4.setOwner(b3);
            //System.out.println("Component: "+f1.getComponentAt(c(b5).getX(), c(b5).getY()));
            assertTrue(b3.containsOrOwns(c(b5).getX(), c(b5).getY()));
            Container c5 = new Container();
            b3.remove();
            c5.add(b3);
            f1.add(c5);
            f1.revalidate();
            assertTrue(c5.containsOrOwns(c(b5).getX(), c(b5).getY()));
            
            
        
        return true;
        }
    
}
