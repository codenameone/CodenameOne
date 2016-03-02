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
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javax.swing.JLabel;
import netscape.javascript.JSObject;

/**
 *
 * @author Chen
 */
public class LocationSimulation extends javax.swing.JFrame {

    private WebView webView;
		private double iLastLat=0.1;
		private double iLastLon=0.1;
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
			/** convert from m/s to km/h */
		private static final double E_Speed2Kmh=3.6;	
    /**
     * single symbol for m/s2 and yd/s2 from symbol font set
     */
    public static final char[] E_MeasUnitS_ms2_Char = {'°', '"', 'h'};
    private static final double[] E_MeasUnitPerM = {1, 1 / 0.91440, 1 / 1 / 0.91440};

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
                mapPanel.setLayout(new BorderLayout());
                mapPanel.add(BorderLayout.CENTER, webContainer);								
                webView.getEngine().loadContent(htmlPage);
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
																								if(Math.abs(newlat-iLastLat)+Math.abs(newlon-iLastLon)>0.000001){
																									iLastLat=newlat;
																									iLastLon=newlon;
																									Preferences p = Preferences.userNodeForPackage(com.codename1.ui.Component.class);
																									p.putDouble("lastGoodLat", newlat);
																									p.putDouble("lastGoodLon", newlon);
																									latitude.setText("" + newlat);
																									longitude.setText("" + newlon);
//																									lang.setText("" + newlat);
//																									longi.setText("" + newlon);
																								}
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
		
		private double getTextVal(String aText){
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
			latitude.setText(aLoc.getLatitude()+"");
			longitude.setText(aLoc.getLongitude()+"");
			velocity.setText(aLoc.getVelocity()+"");
			altitude.setText(aLoc.getAltitude()+"");
			accuracy.setText(aLoc.getAccuracy()+"");
			direction.setText(aLoc.getDirection()+"");
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
      .addGap(0, 512, Short.MAX_VALUE)
    );

    jLabel4.setText("Velocity:");

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
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
              .addComponent(mapPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
              .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                  .addComponent(locationState, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                      .addComponent(jLabel1)
                      .addComponent(jLabel2))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(latitude, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                  .addGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                      .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                      .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                      .addComponent(velocity, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE)
                      .addComponent(altitude, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE)))
                  .addComponent(unit, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 255, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(22, 22, 22))))
          .addGroup(layout.createSequentialGroup()
            .addGap(20, 20, 20)
            .addComponent(jLabel3)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(lang, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jLabel5)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(longi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
          .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
              .addComponent(longitude, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(direction, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addGap(18, 18, 18)
            .addComponent(jLabel8)
            .addGap(4, 4, 4)
            .addComponent(accuracy, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(22, 22, 22)))
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
            .addComponent(altitude))
          .addComponent(longitude, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addGap(1, 1, 1)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
              .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
              .addComponent(accuracy)))
          .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
            .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(direction, javax.swing.GroupLayout.Alignment.LEADING)))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
        .addComponent(mapPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel3)
          .addComponent(lang)
          .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(longi)))
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

	private void moveMap(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
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

    }
  private void latitudeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_latitudeActionPerformed
        moveMap(evt);                                         
  }//GEN-LAST:event_latitudeActionPerformed

  private void velocityActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_velocityActionPerformed
        moveMap(evt);  
  }//GEN-LAST:event_velocityActionPerformed

  private void altitudeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_altitudeActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_altitudeActionPerformed

  private void accuracyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_accuracyActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_accuracyActionPerformed

  private void directionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_directionActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_directionActionPerformed

  private void longitudeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_longitudeActionPerformed
        moveMap(evt); 
  }//GEN-LAST:event_longitudeActionPerformed

  private void locationStateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_locationStateActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_locationStateActionPerformed

  private void unitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unitActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_unitActionPerformed

  private void latitudeMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_latitudeMouseWheelMoved
		latitude.setText(updateWheelMoved(evt, latitude.getText(), 0.001));
		moveMap(null);
  }//GEN-LAST:event_latitudeMouseWheelMoved

  private void longitudeFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_longitudeFocusLost
    moveMap(null);
  }//GEN-LAST:event_longitudeFocusLost

	private String updateWheelMoved(java.awt.event.MouseWheelEvent evt,String aVal,double aDelta){
    int rot=evt.getWheelRotation();
		double n = getTextVal(aVal)-rot*aDelta;
		return n+"";
	}
  private void longitudeMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_longitudeMouseWheelMoved
		longitude.setText(updateWheelMoved(evt, longitude.getText(), 0.001));
		moveMap(null);
  }//GEN-LAST:event_longitudeMouseWheelMoved

  private void directionMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_directionMouseWheelMoved
    direction.setText(updateWheelMoved(evt, direction.getText(), 15.));
  }//GEN-LAST:event_directionMouseWheelMoved

  private void velocityMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_velocityMouseWheelMoved
    velocity.setText(updateWheelMoved(evt, velocity.getText(), 10.));
  }//GEN-LAST:event_velocityMouseWheelMoved

  private void altitudeMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_altitudeMouseWheelMoved
    altitude.setText(updateWheelMoved(evt, altitude.getText(), 100.));
  }//GEN-LAST:event_altitudeMouseWheelMoved

  private void accuracyMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_accuracyMouseWheelMoved
    accuracy.setText(updateWheelMoved(evt, accuracy.getText(), 10.));
  }//GEN-LAST:event_accuracyMouseWheelMoved

  private void latitudeFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_latitudeFocusLost
    moveMap(null);
  }//GEN-LAST:event_latitudeFocusLost

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
		return (float)(getTextVal(accuracy.getText())/E_MeasUnitPerM[unit.getSelectedIndex()]);	
	}

	double getAltitude() {
		return getTextVal(altitude.getText())/E_MeasUnitPerM[unit.getSelectedIndex()];
	}

	float getDirection() {
		return (float)getTextVal(direction.getText());
	}

	float getVelocity() {
		float s=(float)getTextVal(velocity.getText());
		s=(float) (s/E_Speed2Kmh/E_MeasUnitPerKm[unit.getSelectedIndex()]);
		return s;
	}
}
