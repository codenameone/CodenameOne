/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.javase;

import java.awt.BorderLayout;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 *
 * @author Chen
 */
public class LocationSimulation extends javax.swing.JFrame {

    private WebView webView;

    /**
     * Creates new form LocationSimulation
     */
    public LocationSimulation() {
        initComponents();
        pack();
        setLocationByPlatform(true);
        setVisible(true);

        final String htmlPage = "<!DOCTYPE html>\n"
                + "<html>\n"
                + "  <head>\n"
                + "    <meta name=\"viewport\" content=\"initial-scale=1.0, user-scalable=no\" />\n"
                + "    <style type=\"text/css\">\n"
                + "      html { height: 100% }\n"
                + "      body { height: 100%; margin: 0; padding: 0 }\n"
                + "      #map-canvas { height: 100% }\n"
                + "    </style>\n"
                + "    <script type=\"text/javascript\"\n"
                + "      src=\"https://maps.googleapis.com/maps/api/js?key=AIzaSyA6cHeMqVVOHlZ0i9A2oD3jIg56Slvq0Aw&sensor=false\">\n"
                + "    </script>\n"
                + "    <script type=\"text/javascript\">\n"
                + "function moveToLocation(lat, lng){\n"
                + "    var center = new google.maps.LatLng(lat, lng);\n"
                + "    // using global variable:\n"
                + "    document.map.panTo(center);\n"
                + "}"
                + "function initialize() {"
                + "var latlng = new google.maps.LatLng(40.714353, -74.005973 );\n"
                + "var myOptions = {\n"
                + "  zoom: 9,\n"
                + "  center: latlng,\n"
                + "  mapTypeControl: true,\n"
                + "  navigationControl: true,\n"
                + "  streetViewControl: true,\n"
                + "  backgroundColor: \"#FFFFFF\"\n"
                + "};\n"
                + "\n"
                + "document.geocoder = new google.maps.Geocoder();\n"
                + "document.map = new google.maps.Map(document.getElementById(\"map_canvas\"),myOptions);\n"
                + "\n"
                + "document.marker = new google.maps.Marker({\n"
                + "    position: document.map.getCenter(),\n"
                + "    icon: {\n"
                + "      path: google.maps.SymbolPath.CIRCLE,\n"
                + "      scale: 5\n"
                + "    },\n"
                + "    map: document.map\n"
                + "  });"
                + "google.maps.event.addListener(document.map, 'drag', function() { document.marker.setPosition(document.map.getCenter()); } );"
                + "}"
                + "document.updateJavaFX = function updateJavaFX() {\n"
                + "    document.currentCenter  = document.map.getCenter();\n"
                + "    document.currentBounds  = document.map.getBounds();\n"
                + "    document.currentHeading = document.map.getHeading();\n"
                + "    document.currentZoom    = document.map.getZoom();\n"
                + "    document.marker.setPosition(document.currentCenter);\n"
                + "}"
                + "    </script>\n"
                + "  </head>\n"
                + " <body onload=\"initialize()\">\n"
                + "    <div id=\"map_canvas\" style=\"width:100%; height:100%\"></div>\n"
                + " </body>"
                + "</html>";

        final javafx.embed.swing.JFXPanel webContainer = new javafx.embed.swing.JFXPanel();
        Platform.runLater(new Runnable() {

            @Override
            public void run() {
                StackPane root = new StackPane();

                webView = new WebView();
                root.getChildren().add(webView);
                webContainer.setScene(new Scene(root));
                webView.getEngine().loadContent(htmlPage);
                mapPanel.setLayout(new BorderLayout());
                mapPanel.add(BorderLayout.CENTER, webContainer);
                revalidate();

                Timer t = new Timer();
                t.schedule(new TimerTask() {

                    @Override
                    public void run() {

                        Platform.runLater(new Runnable() {

                            public void run() {
                                try {
                                    if (webView != null) {
                                        webView.getEngine().executeScript("document.updateJavaFX()");
                                        JSObject jdoc = (JSObject) webView.getEngine().getDocument();
                                        if (jdoc != null) {
                                            try {
                                                JSObject ds = (JSObject) jdoc.getMember("currentCenter");
                                                String cc = ds.toString().trim();
                                                cc = cc.substring(1, cc.length() - 1);
                                                String[] ccs = cc.split(",");
                                                double newlat = Double.parseDouble(ccs[0].trim());
                                                double newlon = Double.parseDouble(ccs[1].trim());
                                                Preferences p = Preferences.userNodeForPackage(com.codename1.ui.Component.class);
                                                p.putDouble("lastGoodLat", newlat);
                                                p.putDouble("lastGoodLon", newlon);
                                                lang.setText("" + newlat);
                                                longi.setText("" + newlon);
                                            } catch (ClassCastException cce) {
                                                cce.printStackTrace();
                                            }
                                        }
                                    }

                                } catch (Exception e) {

                                }
                            }
                        });

                    }
                }, 1000, 1000);

            }
        });
    }

    public double getLatitude() {
        try {
            String l = lang.getText();
            return Double.valueOf(l);
        } catch (Exception e) {
            return 0;
        }
    }

    public double getLongitude() {
        try {
            String l = longi.getText();
            return Double.valueOf(l);
        } catch (Exception e) {
            return 0;
        }
    }

    public int getState() {
        int index = locationState.getSelectedIndex();
        return index;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        latitude = new javax.swing.JTextField();
        longitude = new javax.swing.JTextField();
        locationState = new javax.swing.JComboBox();
        jButton1 = new javax.swing.JButton();
        mapPanel = new javax.swing.JPanel();
        lang = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        longi = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setText("Latitude");

        jLabel2.setText("Longitude");

        locationState.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Available", "Not-Available", "Temp-Not-Available" }));

        jButton1.setText("Update");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout mapPanelLayout = new javax.swing.GroupLayout(mapPanel);
        mapPanel.setLayout(mapPanelLayout);
        mapPanelLayout.setHorizontalGroup(
            mapPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 380, Short.MAX_VALUE)
        );
        mapPanelLayout.setVerticalGroup(
            mapPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 175, Short.MAX_VALUE)
        );

        jLabel3.setText("Latitude");

        jLabel5.setText("Longitude");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(mapPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(locationState, 0, 282, Short.MAX_VALUE)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel1)
                                            .addComponent(jLabel2))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(latitude)
                                            .addComponent(longitude))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lang, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(longi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addComponent(locationState, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(latitude, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(longitude, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButton1))
                    .addComponent(jLabel2))
                .addGap(22, 22, 22)
                .addComponent(mapPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(lang)
                    .addComponent(jLabel5)
                    .addComponent(longi)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        try {
            String lat = latitude.getText();
            String lon = longitude.getText();

            final Double la = new Double(lat);
            final Double lo = new Double(lon);

            Platform.runLater(new Runnable() {

                public void run() {
                    webView.getEngine().executeScript("moveToLocation(" + la.toString() + "," + lo.toString() + ");");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }//GEN-LAST:event_jButton1ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel lang;
    private javax.swing.JTextField latitude;
    private javax.swing.JComboBox locationState;
    private javax.swing.JLabel longi;
    private javax.swing.JTextField longitude;
    private javax.swing.JPanel mapPanel;
    // End of variables declaration//GEN-END:variables
}
