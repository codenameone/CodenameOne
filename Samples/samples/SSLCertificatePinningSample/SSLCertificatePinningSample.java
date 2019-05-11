package com.codename1.samples;


import com.codename1.io.ConnectionRequest;
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
import com.codename1.io.NetworkManager;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.TextArea;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * This sample demonstrates the use of SSL certificate pinning to verify SSL certificates before sending
 * HTTP requests.
 * 
 * It includes 3 buttons which make 3 different SSL requests.
 */
public class SSLCertificatePinningSample {

    private Form current;
    private Resources theme;
    private boolean trust(ConnectionRequest.SSLCertificate[] certificates) {
        return allowConnections;
    }
    private boolean allowConnections;
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
        Form hi = new Form("Hi World");
        hi.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        
        CheckBox allowConnectionsCb = new CheckBox("Allow Connections");
        allowConnectionsCb.setSelected(true);
        allowConnectionsCb.addActionListener(e->{
            allowConnections = allowConnectionsCb.isSelected();
        });
        hi.add(allowConnectionsCb);
        allowConnections = allowConnectionsCb.isSelected();
        $(hi).append($(new Button("Test Build Request Body"))
                .addActionListener(e->{
                    ConnectionRequest req = new ConnectionRequest() {
                        @Override
                        protected void buildRequestBody(OutputStream os) throws IOException {
                            PrintStream ps = new PrintStream(os);
                            ps.print("Key1=Val1");
                        }

                        
                        
                        @Override
                        protected void checkSSLCertificates(ConnectionRequest.SSLCertificate[] certificates) {
                            /*
                            StringBuilder sb = new StringBuilder();
                            for (SSLCertificate cert : certificates) {
                                System.out.println("Encoding: "+cert.getCertificteAlgorithm()+"; Certificate: "+cert.getCertificteUniqueKey());
                                sb.append("Encoding: "+cert.getCertificteAlgorithm()+"; Certificate: "+cert.getCertificteUniqueKey()).append("\n");
                            }
                            
                            $(()->{
                                $("TextArea")
                                        .setText(sb.toString())
                                        .getComponentForm()
                                        .revalidate();
                                
                            });
                                    */
                            if (!trust(certificates)) {
                                this.kill();
                            }
                        }

                        @Override
                        protected void handleException(Exception err) {
                            err.printStackTrace();
                        }

                        @Override
                        protected void handleErrorResponseCode(int code, String message) {
                            super.handleErrorResponseCode(code, message); //To change body of generated methods, choose Tools | Templates.
                        }
                        
                    };
                    req.setCheckSSLCertificates(true);
                    req.setUrl("https://weblite.ca/tmp/postecho.php");
                    req.setPost(true);
                    req.setHttpMethod("POST");
                    req.addArgument("SomeKey", "SomeValue");
                    //NetworkManager.getInstance().addErrorListener(ne-> {
                    //    ne.getError().printStackTrace();
                    //});
                    //NetworkManager.getInstance().
                    NetworkManager.getInstance().addToQueueAndWait(req);
                    if (req.getResponseCode() == 200) {
                        try {
                            String resp = new String(req.getResponseData(), "UTF-8");
                            String expected = "Post received:\n" +
                                "Array\n" +
                                "(\n" +
                                "    [Key1] => Val1\n" +
                                ")";
                            String passFail = resp.trim().equals(expected.trim()) ? "Test Passed." : "Test Failed";
                            
                            //String expected = ""
                            //resp += "\nExpected: "
                            $(".result", hi).setText(passFail+"\nReceived:\n---------\n"+resp+"\n-----------\nExpected:\n----------\n"+expected+"\n---------\n");
                        } catch (Exception ex) {
                            Log.e(ex);
                        }
                    } else {
                        $(".result", hi).setText("Request failed: "+req.getResponseErrorMessage());
                    }
                                
                })
                .asComponent()
        );
        
        $(hi).append($(new Button("Test Post"))
                .addActionListener(e->{
                    ConnectionRequest req = new ConnectionRequest() {

                        @Override
                        protected void checkSSLCertificates(ConnectionRequest.SSLCertificate[] certificates) {
                            /*
                            StringBuilder sb = new StringBuilder();
                            for (SSLCertificate cert : certificates) {
                                System.out.println("Encoding: "+cert.getCertificteAlgorithm()+"; Certificate: "+cert.getCertificteUniqueKey());
                                sb.append("Encoding: "+cert.getCertificteAlgorithm()+"; Certificate: "+cert.getCertificteUniqueKey()).append("\n");
                            }
                            
                            $(()->{
                                $("TextArea")
                                        .setText(sb.toString())
                                        .getComponentForm()
                                        .revalidate();
                                
                            });
                                    */
                            if (!trust(certificates)) {
                                this.kill();
                            }
                        }

                        @Override
                        protected void handleException(Exception err) {
                            err.printStackTrace();
                        }

                        @Override
                        protected void handleErrorResponseCode(int code, String message) {
                            super.handleErrorResponseCode(code, message); //To change body of generated methods, choose Tools | Templates.
                        }
                        
                    };
                    req.setCheckSSLCertificates(true);
                    req.setUrl("https://weblite.ca/tmp/postecho.php");
                    req.setPost(true);
                    req.setHttpMethod("POST");
                    req.addArgument("SomeKey", "SomeValue");
                    //NetworkManager.getInstance().addErrorListener(ne-> {
                    //    ne.getError().printStackTrace();
                    //});
                    //NetworkManager.getInstance().
                    NetworkManager.getInstance().addToQueueAndWait(req);
                    if (req.getResponseCode() == 200) {
                        try {
                            String resp = new String(req.getResponseData(), "UTF-8");
                            String expected = "Post received:\n" +
                                "Array\n" +
                                "(\n" +
                                "    [SomeKey] => SomeValue\n" +
                                ")";
                            String passFail = resp.trim().equals(expected.trim()) ? "Test Passed." : "Test Failed";
                            
                            //String expected = ""
                            //resp += "\nExpected: "
                            $(".result", hi).setText(passFail+"\nReceived:\n---------\n"+resp+"\n-----------\nExpected:\n----------\n"+expected+"\n---------\n");
                        } catch (Exception ex) {
                            Log.e(ex);
                        }
                    } else {
                        $(".result", hi).setText("Request failed: "+req.getResponseErrorMessage());
                    }
                                
                })
                .asComponent()
        );
        
        $(hi).append($(new Button("Test SSL Certs"))
                .addActionListener(e->{
                    ConnectionRequest req = new ConnectionRequest() {

                        @Override
                        protected void checkSSLCertificates(ConnectionRequest.SSLCertificate[] certificates) {
                            /*
                            StringBuilder sb = new StringBuilder();
                            for (SSLCertificate cert : certificates) {
                                System.out.println("Encoding: "+cert.getCertificteAlgorithm()+"; Certificate: "+cert.getCertificteUniqueKey());
                                sb.append("Encoding: "+cert.getCertificteAlgorithm()+"; Certificate: "+cert.getCertificteUniqueKey()).append("\n");
                            }
                            
                            $(()->{
                                $("TextArea")
                                        .setText(sb.toString())
                                        .getComponentForm()
                                        .revalidate();
                                
                            });
                                    */
                            if (!trust(certificates)) {
                                this.kill();
                            }
                        }

                        @Override
                        protected void handleException(Exception err) {
                            err.printStackTrace();
                        }

                        @Override
                        protected void handleErrorResponseCode(int code, String message) {
                            super.handleErrorResponseCode(code, message); //To change body of generated methods, choose Tools | Templates.
                        }
                        
                    };
                    req.setCheckSSLCertificates(true);
                    req.setUrl("https://confluence.atlassian.com/kb/unable-to-connect-to-ssl-services-due-to-pkix-path-building-failed-779355358.html");
                    //NetworkManager.getInstance().addErrorListener(ne-> {
                    //    ne.getError().printStackTrace();
                    //});
                    //NetworkManager.getInstance().
                    NetworkManager.getInstance().addToQueue(req);
                })
                .asComponent()
        )
        .append($(new TextArea())
                .each(c->{
                    TextArea ta = (TextArea)c;
                    ta.setRows(10);
                })
                .addTags("result")
                .selectAllStyles()
                .setFgColor(0x0)
                .asComponent()
        );

        
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
