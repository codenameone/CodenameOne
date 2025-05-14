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
package com.codename1.impl.javase;

import com.codename1.location.Location;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.*;

/**
 *
 * @author Chen
 */
public class LocationSimulation extends JFrame {

    private WebView webView;
    private WebEngine webEngine;
    private Timer timer;
    private boolean isTextFieldFocused = false;
    private double iLastLat = 0.1;
    private double iLastLon = 0.1;
    public static final int E_MeasUnit_Default = -1;
    public static final int E_MeasUnit_Metric = 0;
    public static final int E_MeasUnit_Imperial = 1;
    public static final int E_MeasUnit_Nautical = 2;
    // measurement unit const Metric (m,km,km/h);Imperial (yd,mi,mph);Nautical(yd,nm,kn)
    private static final String[] E_MeasUnitKmhString = {"km/h", "mph", "kn"};
    private static final String[] E_MeasUnitKmString = {"km", "mi", "nm"};
    private static final double[] E_MeasUnitPerKm = {1, 1 / 1.609344, 1 / 1.8520};
    private static final String[] E_MeasUnitMString = {"m", "yd", "yd"};
    /**
     * single symbol for m/s and yd/s from symbol font set
     */
    public static final char[] E_MeasUnitS_ms_Char = {'\'', 'h', 'h'};
    /**
     * convert from m/s to km/h
     */
    private static final double E_Speed2Kmh = 3.6;
    /**
     * single symbol for m/s2 and yd/s2 from symbol font set
     */
    public static final char[] E_MeasUnitS_ms2_Char = {'"', 'h'};
    private static final double[] E_MeasUnitPerM = {1, 1 / 0.91440, 1 / 1 / 0.91440};
    /**
     * Creates new form LocationSimulation
     */
    public LocationSimulation() {
        initComponents();
        pack();
        setLocationByPlatform(true);
        setVisible(true);

        Preferences p = Preferences.userNodeForPackage(com.codename1.ui.Component.class);
        int startingZoom = p.getInt("lastZoom", 9);
        final String htmlPage = "<!DOCTYPE html>\n"
                + "<html>\n"
                + "  <head>\n"
                + "    <title>Location Simulator</title>\n"
                + "    <meta charset=\"utf-8\" />\n"
                + "    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.7.1/dist/leaflet.css\" />\n"
                + "    <script src=\"https://unpkg.com/leaflet@1.7.1/dist/leaflet.js\"></script>\n"
                + "    <style>\n"
                + "      #map {\n"
                + "        height: 100vh;\n"
                + "        width: 100%;\n"
                + "      }\n"
                + "    </style>\n"
                + "  </head>\n"
                + "  <body>\n"
                + "    <div id=\"map\"></div>\n"
                + "    <script>\n"
                + "      var map = L.map('map').setView([51.505, -0.09], 13);\n"
                + "      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n"
                + "        attribution: '© OpenStreetMap contributors',\n"
                + "        maxZoom: 19\n"
                + "      }).addTo(map);\n"
                + "\n"
                + "      // Function to move the map to a new location\n"
                + "      window.moveToLocation = function(lat, lon) {\n"
                + "        map.setView([lat, lon], map.getZoom());\n"
                + "      };\n"
                + "\n"
                + "      // Function to update JavaFX from JavaScript\n"
                + "      document.updateJavaFX = function() {\n"
                + "        var center = map.getCenter();\n"
                + "        var bounds = map.getBounds();\n"
                + "        var zoom = map.getZoom();\n"
                + "\n"
                + "        // Update global variables\n"
                + "        window.currentCenter = [center.lat, center.lng];\n"
                + "        window.currentZoom = zoom;\n"
                + "        window.currentBounds = bounds.toBBoxString(); // Returns a string representation of the bounds\n"
                + "\n"
                + "        // Update the marker position\n"
                + "        if (document.marker) {\n"
                + "          document.marker.setLatLng(center);\n"
                + "        } else {\n"
                + "          document.marker = L.marker(center).addTo(map);\n"
                + "        }\n"
                + "\n"
                + "        // Return the current map details as a JSON string\n"
                + "        return JSON.stringify({\n"
                + "          lat: center.lat,\n"
                + "          lng: center.lng,\n"
                + "          zoom: zoom\n"
                + "        });\n"
                + "      };\n"
                + "\n"
                + "      // Add event listeners to update currentCenter and currentZoom\n"
                + "      map.on('move', function() {\n"
                + "        document.updateJavaFX();\n"
                + "      });\n"
                + "\n"
                + "      map.on('drag', function() {\n"
                + "        document.updateJavaFX();\n"
                + "      });\n"
                + "\n"
                + "      map.on('zoom', function() {\n"
                + "        document.updateJavaFX();\n"
                + "      });\n"
                + "\n"
                + "      // Initialize the marker\n"
                + "      document.marker = L.marker([51.505, -0.09]).addTo(map);\n"
                + "    </script>\n"
                + "  </body>\n"
                + "</html>";

        final JFXPanel webContainer = new JFXPanel();
        mapPanel.setLayout(new BorderLayout());
        mapPanel.add(BorderLayout.CENTER, webContainer);

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                StackPane root = new StackPane();
                webView = new WebView();
                webEngine = webView.getEngine();
                root.getChildren().add(webView);
                webContainer.setScene(new Scene(root));

                // Load the HTML content
                webEngine.loadContent(htmlPage, "text/html");

                // Add a listener for the load state
                webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
                    @Override
                    public void changed(ObservableValue<? extends Worker.State> obs,
                                        Worker.State oldState,
                                        Worker.State newState) {
                        if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                            // Script is fully loaded, you can now call moveToLocation
                            timer = new Timer();
                            timer.scheduleAtFixedRate(new TimerTask() {
                                @Override
                                public void run() {
                                    if (!isTextFieldFocused) {
                                        Platform.runLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    Object result = webEngine.executeScript("document.updateJavaFX()");

                                                    if (result != null) {
                                                        // Parse the coordinates more safely
                                                        String jsonStr = result.toString();
                                                        // Remove the curly braces and quotes
                                                        jsonStr = jsonStr.replaceAll("[{}\"]", "");

                                                        // Create a map to store our values
                                                        Map<String, Double> values = new HashMap<>();

                                                        // Split by comma and process each key-value pair
                                                        for (String pair : jsonStr.split(",")) {
                                                            try {
                                                                String[] keyValue = pair.trim().split(":");
                                                                if (keyValue.length == 2) {  // Make sure we have both key and value
                                                                    String key = keyValue[0].trim();
                                                                    double value = Double.parseDouble(keyValue[1].trim());
                                                                    values.put(key, value);
                                                                }
                                                            } catch (Exception e) {
                                                                // Skip this pair if there's an error
                                                                continue;
                                                            }
                                                        }

                                                        // Only update if we have both latitude and longitude
                                                        if (values.containsKey("lat") && values.containsKey("lng")) {
                                                            double newLat = values.get("lat");
                                                            double newLon = values.get("lng");
                                                            int zoom = values.getOrDefault("zoom", 13.0).intValue();

                                                            // Update preferences
                                                            Preferences prefs = Preferences.userNodeForPackage(com.codename1.ui.Component.class);
                                                            prefs.putInt("lastZoom", zoom);
                                                            prefs.putDouble("lastGoodLat", newLat);
                                                            prefs.putDouble("lastGoodLon", newLon);

                                                            // Update the text fields
                                                            latitude.setText(String.format("%.6f", newLat));
                                                            longitude.setText(String.format("%.6f", newLon));
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                                    }
                                }
                            }, 1000, 1000);

                            // Add focus listeners to the text fields
                            latitude.addFocusListener(new java.awt.event.FocusListener() {
                                @Override
                                public void focusGained(java.awt.event.FocusEvent e) {
                                    isTextFieldFocused = true;
                                }

                                @Override
                                public void focusLost(java.awt.event.FocusEvent e) {
                                    isTextFieldFocused = false;
                                }
                            });

                            longitude.addFocusListener(new java.awt.event.FocusListener() {
                                @Override
                                public void focusGained(java.awt.event.FocusEvent e) {
                                    isTextFieldFocused = true;
                                }

                                @Override
                                public void focusLost(java.awt.event.FocusEvent e) {
                                    isTextFieldFocused = false;
                                }
                            });
                        }
                    }

                });
            }
        });
    }

    private double getTextVal(String aText) {
        try {
            String l = aText;
            return Double.valueOf(l);
        } catch (Exception e) {
            return 0;
        }
    }

    public double getLatitude() {
        return getTextVal(latitude.getText());
//        try {
//            String l = lang.getText();
//            return Double.valueOf(l);
//        } catch (Exception e) {
//            return 0;
//        }
    }

    public double getLongitude() {
        return getTextVal(longitude.getText());
//        try {
//            String l = longi.getText();
//            return Double.valueOf(l);
//        } catch (Exception e) {
//            return 0;
//        }
    }

    public int getState() {
        int index = locationState.getSelectedIndex();
        return index;
    }

    public void setLocation(Location aLoc) {
        locationState.setSelectedIndex(aLoc.getStatus());
        latitude.setText(aLoc.getLatitude() + "");
        longitude.setText(aLoc.getLongitude() + "");
        setVelocity(aLoc.getVelocity());
        setAltitude(aLoc.getAltitude());
        setAccuracy(aLoc.getAccuracy());
        direction.setText(aLoc.getDirection() + "");
        locationState.setSelectedIndex(aLoc.getStatus());
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
        mapPanel = new javax.swing.JPanel();
        lang = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        longi = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        altitude = new javax.swing.JTextField();
        velocity = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        direction = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        accuracy = new javax.swing.JTextField();
        unit = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setText("Latitude:");

        jLabel2.setText("Longitude:");

        latitude.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                latitudeFocusLost(evt);
            }
        });
        latitude.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                latitudeMouseWheelMoved(evt);
            }
        });
        latitude.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                latitudeActionPerformed(evt);
            }
        });

        longitude.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                longitudeFocusLost(evt);
            }
        });
        longitude.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                longitudeMouseWheelMoved(evt);
            }
        });
        longitude.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                longitudeActionPerformed(evt);
            }
        });

        locationState.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Available", "Not-Available", "Temp-Not-Available" }));
        locationState.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                locationStateActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout mapPanelLayout = new javax.swing.GroupLayout(mapPanel);
        mapPanel.setLayout(mapPanelLayout);
        mapPanelLayout.setHorizontalGroup(
            mapPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        mapPanelLayout.setVerticalGroup(
            mapPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 595, Short.MAX_VALUE)
        );

        jLabel4.setText("Velocity:");

        altitude.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                altitudeFocusLost(evt);
            }
        });
        altitude.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                altitudeMouseWheelMoved(evt);
            }
        });
        altitude.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                altitudeActionPerformed(evt);
            }
        });

        velocity.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                velocityFocusLost(evt);
            }
        });
        velocity.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                velocityMouseWheelMoved(evt);
            }
        });
        velocity.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                velocityActionPerformed(evt);
            }
        });

        jLabel6.setText("Altitude:");

        jLabel7.setText("Direction:");

        direction.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                directionFocusLost(evt);
            }
        });
        direction.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                directionMouseWheelMoved(evt);
            }
        });
        direction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                directionActionPerformed(evt);
            }
        });

        jLabel8.setText("Accuracy:");

        accuracy.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                accuracyFocusLost(evt);
            }
        });
        accuracy.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                accuracyMouseWheelMoved(evt);
            }
        });
        accuracy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                accuracyActionPerformed(evt);
            }
        });

        unit.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Metric [m],[km/h]", "Imperial [yd],[mph]", "Nautical [yd],[kn]" }));
        unit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unitActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lang, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(jLabel5)
                        .addGap(0, 0, 0)
                        .addComponent(longi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(mapPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(locationState, 0, 298, Short.MAX_VALUE)
                                            .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(jLabel1)
                                                    .addComponent(jLabel2))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(latitude)
                                                    .addComponent(longitude)
                                                    .addComponent(direction))))
                                        .addGap(18, 18, 18))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(173, 173, 173)))
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(unit, 0, 309, Short.MAX_VALUE)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addComponent(jLabel8))
                                        .addGap(1, 1, 1)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(accuracy)
                                            .addComponent(velocity)
                                            .addComponent(altitude))))))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(locationState, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(unit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(velocity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(latitude, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(altitude)
                        .addComponent(longitude, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(direction)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(accuracy)
                        .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(mapPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(lang)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(longi)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void updateSave(java.awt.event.ActionEvent evt) {
        try {

            String lat = latitude.getText();
            String lon = longitude.getText();

            if (lat.length() == 0 || lon.length() == 0) {
                return;
            }

            final Double la = new Double(lat);
            final Double lo = new Double(lon);

            Platform.runLater(new Runnable() {

                public void run() {
                    Preferences p = Preferences.userNodeForPackage(com.codename1.ui.Component.class);
                    p.putFloat("accuracy", getAccuracy());
                    p.putFloat("velocity", getVelocity());
                    p.putDouble("Altitude", getAltitude());
                    p.putFloat("direction", getDirection());
                    webEngine.executeScript("moveToLocation(" + la.toString() + "," + lo.toString() + ");");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

  private void latitudeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_latitudeActionPerformed
      updateSave(evt);
  }//GEN-LAST:event_latitudeActionPerformed

  private void velocityActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_velocityActionPerformed
      updateSave(evt);
  }//GEN-LAST:event_velocityActionPerformed

  private void altitudeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_altitudeActionPerformed
      updateSave(evt);
  }//GEN-LAST:event_altitudeActionPerformed

  private void accuracyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_accuracyActionPerformed
      updateSave(evt);
  }//GEN-LAST:event_accuracyActionPerformed

  private void directionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_directionActionPerformed
      updateSave(evt);
  }//GEN-LAST:event_directionActionPerformed

  private void longitudeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_longitudeActionPerformed
      updateSave(evt);
  }//GEN-LAST:event_longitudeActionPerformed

  private void locationStateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_locationStateActionPerformed
      /*Platform.runLater(new Runnable() {

          public void run() {*/
              Preferences p = Preferences.userNodeForPackage(com.codename1.ui.Component.class);
              p.putInt("state", locationState.getSelectedIndex());
      /*    }
      });*/
  }//GEN-LAST:event_locationStateActionPerformed

  private void unitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unitActionPerformed
      /*Platform.runLater(new Runnable() {
          public void run() {*/
              Preferences p = Preferences.userNodeForPackage(com.codename1.ui.Component.class);
              p.putInt("unit", unit.getSelectedIndex());
      /*    }
      });*/
      // updateSave(evt);    
  }//GEN-LAST:event_unitActionPerformed

  private void latitudeMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_latitudeMouseWheelMoved
      latitude.setText(updateWheelMoved(evt, latitude.getText(), 0.001));
      updateSave(null);
  }//GEN-LAST:event_latitudeMouseWheelMoved

  private void longitudeFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_longitudeFocusLost
      updateSave(null);
  }//GEN-LAST:event_longitudeFocusLost

    private String updateWheelMoved(java.awt.event.MouseWheelEvent evt, String aVal, double aDelta) {
        int rot = evt.getWheelRotation();
        double n = getTextVal(aVal) - rot * aDelta;
        return n + "";
    }
  private void longitudeMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_longitudeMouseWheelMoved
      longitude.setText(updateWheelMoved(evt, longitude.getText(), 0.001));
      updateSave(null);
  }//GEN-LAST:event_longitudeMouseWheelMoved

  private void directionMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_directionMouseWheelMoved
      direction.setText(updateWheelMoved(evt, direction.getText(), 15.));
      updateSave(null);
  }//GEN-LAST:event_directionMouseWheelMoved

  private void velocityMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_velocityMouseWheelMoved
      velocity.setText(updateWheelMoved(evt, velocity.getText(), 10.));
      updateSave(null);
  }//GEN-LAST:event_velocityMouseWheelMoved

  private void altitudeMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_altitudeMouseWheelMoved
      altitude.setText(updateWheelMoved(evt, altitude.getText(), 100.));
  }//GEN-LAST:event_altitudeMouseWheelMoved

  private void accuracyMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_accuracyMouseWheelMoved
      accuracy.setText(updateWheelMoved(evt, accuracy.getText(), 10.));
      updateSave(null);
  }//GEN-LAST:event_accuracyMouseWheelMoved

  private void latitudeFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_latitudeFocusLost
      updateSave(null);
  }//GEN-LAST:event_latitudeFocusLost

  private void velocityFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_velocityFocusLost
      updateSave(null);
  }//GEN-LAST:event_velocityFocusLost

  private void altitudeFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_altitudeFocusLost
      updateSave(null);
  }//GEN-LAST:event_altitudeFocusLost

  private void accuracyFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_accuracyFocusLost
      updateSave(null);
  }//GEN-LAST:event_accuracyFocusLost

  private void directionFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_directionFocusLost
      updateSave(null);
  }//GEN-LAST:event_directionFocusLost


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField accuracy;
    private javax.swing.JTextField altitude;
    private javax.swing.JTextField direction;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel lang;
    private javax.swing.JTextField latitude;
    private javax.swing.JComboBox locationState;
    private javax.swing.JLabel longi;
    private javax.swing.JTextField longitude;
    private javax.swing.JPanel mapPanel;
    private javax.swing.JComboBox unit;
    private javax.swing.JTextField velocity;
    // End of variables declaration//GEN-END:variables

    float getAccuracy() {
        return (float) (getTextVal(accuracy.getText()) / E_MeasUnitPerM[unit.getSelectedIndex()]);
    }

    double getAltitude() {
        return getTextVal(altitude.getText()) / E_MeasUnitPerM[unit.getSelectedIndex()];
    }

    float getDirection() {
        return (float) getTextVal(direction.getText());
    }

    float getVelocity() {
        float s = (float) getTextVal(velocity.getText());
        s = (float) (s / E_Speed2Kmh / E_MeasUnitPerKm[unit.getSelectedIndex()]);
        return s;
    }

    void setAccuracy(float aValue) {
        accuracy.setText((aValue * E_MeasUnitPerM[unit.getSelectedIndex()]) + "");
    }

    void setAltitude(double aValue) {
        altitude.setText((aValue * E_MeasUnitPerM[unit.getSelectedIndex()]) + "");
    }

    void setVelocity(float aValue) {
        velocity.setText((aValue * E_Speed2Kmh * E_MeasUnitPerKm[unit.getSelectedIndex()]) + "");
    }

    void setMeasUnit(int aInt) {
        unit.setSelectedIndex(aInt);
    }
}
