/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */
package com.codename1.impl.javase.fx;

import com.codename1.impl.javase.AbstractBrowserWindowSE;
import com.codename1.io.Log;
import com.codename1.ui.BrowserWindow;
import com.codename1.ui.events.ActionEvent;
import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * A Browser Window implementation that uses JavaFX components.
 * @author shannah
 * @since 7.0
 */
public class FXBrowserWindowSE extends AbstractBrowserWindowSE {
    private Stage stage;
    private WebView webview;
    private Worker.State state;
    
    
    public FXBrowserWindowSE(String startURL) {
        try {
            initUI(startURL);
        } catch (IllegalStateException ex) {
            try {
                EventQueue.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        new JFXPanel();
                    }

                });
            } catch (InterruptedException iex) {
                Log.e(iex);
                throw ex;
            } catch (InvocationTargetException ite) {
                Log.e(ite);
                throw ex;
            }
            initUI(startURL);
        }
    }
    
    private void initUI(final String startURL) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(new Runnable() {
                public void run() {
                    initUI(startURL);
                }
            });
            return;
        }
        webview = new WebView();
        webview.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue<? extends Worker.State> ov, Worker.State t, Worker.State t1) {
                state = t1;
                
                if (t1 == Worker.State.SUCCEEDED) {
                    fireLoadEvent(new ActionEvent(webview.getEngine().locationProperty().get()));
                }
            }
            
        });
        
        StackPane stackPane = new StackPane();
        stackPane.getChildren().add(webview);
        Scene scene = new Scene(stackPane);
        stage = new Stage();
        stage.setOnHidden(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                fireCloseEvent(new ActionEvent(null));
            }
        }) ;
        stage.setScene(scene);
        webview.getEngine().load(startURL);
    }
    
   
    
    
    
    
    @Override
    public void show() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(new Runnable() {
                public void run() {
                    show();
                }
            });
            return;
        }
        stage.show();
    }

    @Override
    public void setSize(final int width, final int height) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(new Runnable() {
                public void run() {
                    setSize(width, height);
                }
            });
            return;
        }
        stage.setWidth(width);
        stage.setHeight(height);
    }
    
    @Override
    public void setTitle(final String title) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(new Runnable() {
                public void run() {
                    setTitle(title);
                }
            });
            return;
        }
        stage.setTitle(title);
    }

    @Override
    public void hide() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(new Runnable() {
                public void run() {
                    hide();
                }
            });
            return;
        }
        stage.hide();
    }

    @Override
    public void cleanup() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(new Runnable() {
                public void run() {
                    cleanup();
                }
            });
            return;
        }
        stage.close();
    }

    @Override
    public void eval(BrowserWindow.EvalRequest req) {
        req.error(new RuntimeException("Not implemented"));
    }

}
