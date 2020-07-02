/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase.cef;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.impl.javase.JavaSEPort.CN1JPanel;
import com.codename1.impl.javase.PeerComponentBuffer;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CN1CefBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefFocusHandlerAdapter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefCookieManager;

/**
 *
 * @author shannah
 */
public class BrowserPanel extends CN1JPanel {
    
    private boolean isClosed_ = false;
    private CefBrowser browser_ = null;
    private static int browserCount_ = 0;
    private Runnable afterParentChangedAction_ = null;
    private String title_ = null;
    private String url_ = null;
    private final CefClient client_;
    private String errorMsg_ = "";
    private CEFPeerComponentBuffer buffer_;
    private boolean browserFocus_ = true;
    private BrowserComponent browserComponent;
    private CEFBrowserComponent cefBrowserComponent;
    
    public BrowserPanel(CEFPeerComponentBuffer buffer, BrowserComponent browserComponent, boolean osrEnabled, boolean transparentPaintingEnabled,
            boolean createImmediately, String[] args) {
        setZoom(1);
        this.browserComponent = browserComponent;
        this.buffer_ = buffer;
        setLayout(new CardLayout());
        
        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent arg0) {
                if (browser_ != null) {
                    browser_.getUIComponent().requestFocus();
                }
            }

            @Override
            public void focusLost(FocusEvent arg0) {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
            
        });
        
        // Browser window closing works as follows:
        //   1. Clicking the window X button calls WindowAdapter.windowClosing.
        //   2. WindowAdapter.windowClosing calls CefBrowser.close(false).
        //   3. CEF calls CefLifeSpanHandler.doClose() which calls CefBrowser.doClose()
        //      which returns true (canceling the close).
        //   4. CefBrowser.doClose() triggers another call to WindowAdapter.windowClosing.
        //   5. WindowAdapter.windowClosing calls CefBrowser.close(true).
        //   6. For windowed browsers CEF destroys the native window handle. For OSR
        //      browsers CEF calls CefLifeSpanHandler.doClose() which calls
        //      CefBrowser.doClose() again which returns false (allowing the close).
        //   7. CEF calls CefLifeSpanHandler.onBeforeClose and the browser is destroyed.
        //
        // On macOS pressing Cmd+Q results in a call to CefApp.handleBeforeTerminate
        // which calls CefBrowser.close(true) for each existing browser. CEF then calls
        // CefLifeSpanHandler.onBeforeClose and the browser is destroyed.
        //
        // Application shutdown works as follows:
        //   1. CefLifeSpanHandler.onBeforeClose calls CefApp.getInstance().dispose()
        //      when the last browser window is destroyed.
        //   2. CefAppHandler.stateHasChanged terminates the application by calling
        //      System.exit(0) when the state changes to CefAppState.TERMINATED.
        /*
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (browser_ == null) {
                    // If there's no browser we can dispose immediately.
                    isClosed_ = true;
                    System.out.println("BrowserFrame.windowClosing Frame.dispose");
                    dispose();
                    return;
                }

                boolean isClosed = isClosed_;

                if (isClosed) {
                    // Cause browser.doClose() to return false so that OSR browsers
                    // can close.
                    browser_.setCloseAllowed();
                }

                // Results in another call to this method.
                System.out.println("BrowserFrame.windowClosing CefBrowser.close(" + isClosed + ")");
                browser_.close(isClosed);
                if (!isClosed_) {
                    isClosed_ = true;
                }
                if (isClosed) {
                    // Dispose after the 2nd call to this method.
                    System.out.println("BrowserFrame.windowClosing Frame.dispose");
                    dispose();
                }
            }
        });
        */
        
        
        CefApp myApp;
        if (CefApp.getState() != CefApp.CefAppState.INITIALIZED) {
            // 1) CefApp is the entry point for JCEF. You can pass
            //    application arguments to it, if you want to handle any
            //    chromium or CEF related switches/attributes in
            //    the native world.
            CefSettings settings = new CefSettings();
            settings.windowless_rendering_enabled = osrEnabled;
            // try to load URL "about:blank" to see the background color
            settings.background_color = settings.new ColorType(100, 255, 242, 211);
            //settings.user_agent = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/83.0.4103.88 Mobile/15E148 Safari/604.1";
            settings.user_agent = CN.getProperty("user-agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 13_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/83.0.4103.88 Mobile/15E148 Safari/604.1" );
            settings.remote_debugging_port = 8088;
            myApp = CefApp.getInstance(args, settings);

            CefApp.CefVersion version = myApp.getVersion();
            System.out.println("Using:\n" + version);

            //    We're registering our own AppHandler because we want to
            //    add an own schemes (search:// and client://) and its corresponding
            //    protocol handlers. So if you enter "search:something on the web", your
            //    search request "something on the web" is forwarded to www.google.com
            CefApp.addAppHandler(new AppHandler(args));
        } else {
            myApp = CefApp.getInstance();
        }

        //    By calling the method createClient() the native part
        //    of JCEF/CEF will be initialized and an  instance of
        //    CefClient will be created. You can create one to many
        //    instances of CefClient.
        client_ = myApp.createClient();

        // 2) You have the ability to pass different handlers to your
        //    instance of CefClient. Each handler is responsible to
        //    deal with different informations (e.g. keyboard input).
        //
        //    For each handler (with more than one method) adapter
        //    classes exists. So you don't need to override methods
        //    you're not interested in.
        //DownloadDialog downloadDialog = new DownloadDialog(this);
        client_.addContextMenuHandler(new ContextMenuHandler(this));
        //client_.addDownloadHandler(downloadDialog);
        client_.addDragHandler(new DragHandler());
        client_.addJSDialogHandler(new JSDialogHandler());
        client_.addKeyboardHandler(new KeyboardHandler());
        client_.addRequestHandler(new RequestHandler(this, browserComponent));
        

        //    Beside the normal handler instances, we're registering a MessageRouter
        //    as well. That gives us the opportunity to reply to JavaScript method
        //    calls (JavaScript binding). We're using the default configuration, so
        //    that the JavaScript binding methods "cefQuery" and "cefQueryCancel"
        //    are used.
        CefMessageRouter msgRouter = CefMessageRouter.create();
        msgRouter.addHandler(new MessageRouterHandler(browserComponent), true);
        //msgRouter.addHandler(new MessageRouterHandlerEx(client_), false);
        client_.addMessageRouter(msgRouter);

        // 2.1) We're overriding CefDisplayHandler as nested anonymous class
        //      to update our address-field, the title of the panel as well
        //      as for updating the status-bar on the bottom of the browser
        client_.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
                //control_pane_.setAddress(browser, url);
                System.out.println("Address changed to "+url);
                url_ = url;
            }
            @Override
            public void onTitleChange(CefBrowser browser, String title) {
                setTitle(title);
            }
            @Override
            public void onStatusMessage(CefBrowser browser, String value) {
                //status_panel_.setStatusText(value);
            }
        });

        // 2.2) To disable/enable navigation buttons and to display a prgress bar
        //      which indicates the load state of our website, we're overloading
        //      the CefLoadHandler as nested anonymous class. Beside this, the
        //      load handler is responsible to deal with (load) errors as well.
        //      For example if you navigate to a URL which does not exist, the
        //      browser will show up an error message.
        client_.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadingStateChange(CefBrowser browser, boolean isLoading,
                    boolean canGoBack, boolean canGoForward) {
                //control_pane_.update(browser, isLoading, canGoBack, canGoForward);
                //status_panel_.setIsInProgress(isLoading);
                if (browserComponent != null) {
                    if (isLoading) {
                        //System.out.println("Firing onStart "+url_);
                        browserComponent.fireWebEvent(BrowserComponent.onStart, new ActionEvent(url_));
                    } else {
                        //System.out.println("Firing onLoad "+url_);
                        //String bootstrap = "if (!window.cn1application) window.cn1application={};"
                        //        + "window.cn1application."
                        
                        browser_.executeJavaScript("window.cn1application = ", url_, ERROR);
                        browserComponent.fireWebEvent(BrowserComponent.onLoad, new ActionEvent(url_));
                       
                    }
                }

            }

            @Override
            public void onLoadError(CefBrowser browser, CefFrame frame, CefLoadHandler.ErrorCode errorCode,
                    String errorText, String failedUrl) {
                if (browserComponent != null) {
                    browserComponent.fireWebEvent(BrowserComponent.onError, new ActionEvent(errorText, errorCode.getCode()));
                }
                if (errorCode != CefLoadHandler.ErrorCode.ERR_NONE && errorCode != CefLoadHandler.ErrorCode.ERR_ABORTED) {
                    errorMsg_ = "<html><head>";
                    errorMsg_ += "<title>Error while loading</title>";
                    errorMsg_ += "</head><body>";
                    errorMsg_ += "<h1>" + errorCode + "</h1>";
                    errorMsg_ += "<h3>Failed to load " + failedUrl + "</h3>";
                    errorMsg_ += "<p>" + (errorText == null ? "" : errorText) + "</p>";
                    errorMsg_ += "</body></html>";
                    browser.stopLoad();
                }
            }
        });

        // Create the browser.
        CN1CefBrowser.setComponentFactory(new CEFComponentFactory());
        CN1CefBrowser.setUIPlatform(new CEFUIPlatform());
        CefBrowser browser = client_.createBrowser(
                "http://www.youtube.com", osrEnabled, transparentPaintingEnabled, null);
        ((CN1CefBrowser)browser).setPeerComponentBuffer(buffer);

        setBrowser(browser);

        // Set up the UI for this example implementation.
        /*
        JPanel contentPanel = createContentPanel();
        getContentPane().add(contentPanel, BorderLayout.CENTER);

        // Clear focus from the browser when the address field gains focus.
        control_pane_.getAddressField().addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!browserFocus_) return;
                browserFocus_ = false;
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                control_pane_.getAddressField().requestFocus();
            }
        });
        */
        // Clear focus from the address field when the browser gains focus.
        client_.addFocusHandler(new CefFocusHandlerAdapter() {
            @Override
            public void onGotFocus(CefBrowser browser) {
                if (browserFocus_) return;
                browserFocus_ = true;
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                browser.setFocus(true);
            }

            @Override
            public void onTakeFocus(CefBrowser browser, boolean next) {
                browserFocus_ = false;
            }
        });

        if (createImmediately) browser.createImmediately();
        
        // Add the browser to the UI.
        add(getBrowser().getUIComponent());
        add(new JButton("This is a button"));

        /*
        MenuBar menuBar = new MenuBar(
                this, browser, control_pane_, downloadDialog, CefCookieManager.getGlobalManager());

        menuBar.addBookmark("Binding Test", "client://tests/binding_test.html");
        menuBar.addBookmark("Binding Test 2", "client://tests/binding_test2.html");
        menuBar.addBookmark("Download Test", "http://opensource.spotify.com/cefbuilds/index.html");
        menuBar.addBookmark("Login Test (username:pumpkin, password:pie)",
                "http://www.colostate.edu/~ric/protect/your.html");
        menuBar.addBookmark("Certificate-error Test", "https://www.k2go.de");
        menuBar.addBookmark("Resource-Handler Test", "http://www.foo.bar/");
        menuBar.addBookmark("Resource-Handler Set Error Test", "http://seterror.test/");
        menuBar.addBookmark(
                "Scheme-Handler Test 1: (scheme \"client\")", "client://tests/handler.html");
        menuBar.addBookmark(
                "Scheme-Handler Test 2: (scheme \"search\")", "search://do a barrel roll/");
        menuBar.addBookmark("Spellcheck Test", "client://tests/spellcheck.html");
        menuBar.addBookmark("LocalStorage Test", "client://tests/localstorage.html");
        menuBar.addBookmark("Transparency Test", "client://tests/transparency.html");
        menuBar.addBookmarkSeparator();
        menuBar.addBookmark(
                "javachromiumembedded", "https://bitbucket.org/chromiumembedded/java-cef");
        menuBar.addBookmark("chromiumembedded", "https://bitbucket.org/chromiumembedded/cef");
        setJMenuBar(menuBar);
        */
    }
    
    public void setBrowser(CefBrowser browser) {
        if (browser_ == null) browser_ = browser;

        browser_.getClient().removeLifeSpanHandler();
        browser_.getClient().addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public void onAfterCreated(CefBrowser browser) {
                System.out.println("BrowserFrame.onAfterCreated id=" + browser.getIdentifier());
                browserCount_++;
                if (cefBrowserComponent != null) {
                    cefBrowserComponent.fireReady();
                }
            }

            @Override
            public void onAfterParentChanged(CefBrowser browser) {
                System.out.println(
                        "BrowserFrame.onAfterParentChanged id=" + browser.getIdentifier());
                if (afterParentChangedAction_ != null) {
                    SwingUtilities.invokeLater(afterParentChangedAction_);
                    afterParentChangedAction_ = null;
                }
            }

            @Override
            public boolean doClose(CefBrowser browser) {
                boolean result = browser.doClose();
                System.out.println("BrowserFrame.doClose id=" + browser.getIdentifier()
                        + " CefBrowser.doClose=" + result);
                return result;
            }

            @Override
            public void onBeforeClose(CefBrowser browser) {
                System.out.println("BrowserFrame.onBeforeClose id=" + browser.getIdentifier());
                if (--browserCount_ == 0) {
                    System.out.println("BrowserFrame.onBeforeClose CefApp.dispose");
                    CefApp.getInstance().dispose();
                }
            }
        });
    }

    public void removeBrowser(Runnable r) {
        System.out.println("BrowserFrame.removeBrowser");
        afterParentChangedAction_ = r;
        remove(browser_.getUIComponent());
        // The removeNotify() notification should be sent as a result of calling remove().
        // However, it isn't in all cases so we do it manually here.
        browser_.getUIComponent().removeNotify();
        browser_ = null;
    }

    public CefBrowser getBrowser() {
        return browser_;
    }
    
    public void setTitle(String title) {
        title_ = title;
    }
    
    public String getTitle() {
        return title_;
    }
    
    public String getURL() {
        return url_;
    }

    public void setBrowserComponent(BrowserComponent parent) {
        
        this.browserComponent = parent;
    }
    
    public void setCEFBrowserComponent(CEFBrowserComponent cefBc) {
        this.cefBrowserComponent = cefBc;
    }

   
    
    
    
    
    
    
    
    
    
    
    
}
