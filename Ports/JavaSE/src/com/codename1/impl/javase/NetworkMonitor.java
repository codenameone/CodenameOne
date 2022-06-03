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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.IOAccessor;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Allows viewing network activity from the executing application
 *
 * @author Shai Almog
 */
public class NetworkMonitor extends javax.swing.JPanel {

    private Map<URLConnection, NetworkRequestObject> requests = new HashMap<URLConnection, NetworkRequestObject>();
    private Map<ConnectionRequest, NetworkRequestObject> queuedRequests = new HashMap<ConnectionRequest, NetworkRequestObject>();
    private JFrame frame;
    /** Creates new form NetworkMonitor */
    public NetworkMonitor() {
        initComponents();
        requestHeaders.setLineWrap(true);
        postBody.setLineWrap(true);
        responseBody.setLineWrap(true);
        responseHeaders.setLineWrap(true);
        
        request.setModel(new DefaultListModel());
        request.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList jlist, Object o, int i, boolean bln, boolean bln1) {
                NetworkRequestObject nr = (NetworkRequestObject)o;
                if(nr.getMethod() != null) {
                    o = nr.getMethod() + " - " + nr.getUrl();
                } else {
                    o = nr.getUrl();
                }
                return super.getListCellRendererComponent(jlist, o, i, bln, bln1);
            }
            
        });
        request.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent lse) {
                NetworkRequestObject nr = (NetworkRequestObject)request.getSelectedValue();
                
            }
        });
    }

    public JFrame showInNewWindow() {
        if (frame == null) {
            frame = new JFrame("Network Monitor");
            frame.getContentPane().setLayout(new java.awt.BorderLayout());
            frame.getContentPane().add(this, java.awt.BorderLayout.CENTER);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    JavaSEPort.disableNetworkMonitor();
                }
            });

            frame.pack();
            frame.setLocationByPlatform(true);
            frame.setVisible(true);

        }
        return frame;
    }

    public void dispose() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    public synchronized void addRequest(URLConnection con, NetworkRequestObject r) {
        requests.put(con, r);
        ((DefaultListModel)request.getModel()).addElement(r);
    }
    
    public synchronized void addQueuedRequest(ConnectionRequest req, NetworkRequestObject r) {
        queuedRequests.put(req, r);
       
    }
    
    public synchronized void removeQueuedRequest(NetworkRequestObject r) {
        ConnectionRequest key = null;
        for (ConnectionRequest req : queuedRequests.keySet()) {
            if (queuedRequests.get(req) == r) {
                key = req;
                break;
            }
        }
        if (key != null) {
            queuedRequests.remove(key);
        }
    }
   
    public synchronized NetworkRequestObject findQueuedRequest(int id) {
        for (ConnectionRequest req : queuedRequests.keySet()) {
            if (id == IOAccessor.getId(req)) {
                return queuedRequests.get(req);
            }
        }
        return null;
    }
    
    public synchronized NetworkRequestObject getByConnectionRequest(ConnectionRequest req) {
        return queuedRequests.get(req);
    }
    
    public synchronized NetworkRequestObject getByConnection(URLConnection con) {
        return requests.get(con);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        request = new javax.swing.JList();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        requestType = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        responseCode = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        responseLength = new javax.swing.JLabel();
        url = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        requestTime = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        responseTime = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        downloadTime = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        totalTime = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        timeQueued = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        postBody = new javax.swing.JTextArea();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        requestHeaders = new javax.swing.JTextArea();
        jPanel3 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        responseBody = new javax.swing.JTextArea();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        responseHeaders = new javax.swing.JTextArea();
        jPanel4 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();

        FormListener formListener = new FormListener();

        //setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        //setTitle("Network Monitor");
        //addWindowListener(formListener);

        jSplitPane1.setContinuousLayout(true);
        jSplitPane1.setOneTouchExpandable(true);

        request.addListSelectionListener(formListener);
        jScrollPane1.setViewportView(request);

        jSplitPane1.setLeftComponent(jScrollPane1);

        jLabel1.setText("URL");

        jLabel2.setText("Type");

        requestType.setText("POST");

        jLabel5.setText("Response Code");

        responseCode.setText("200");

        jLabel8.setText("Response Length");

        responseLength.setText("-1");

        url.setEditable(false);

        jLabel9.setText("Request Time");

        requestTime.setText("-1");

        jLabel11.setText("Response Time");

        responseTime.setText("-1");

        jLabel13.setText("Download Time");

        downloadTime.setText("-1");

        jLabel15.setText("Total Time");

        totalTime.setText("-1");

        jLabel16.setText("Time Queued");

        timeQueued.setText("-1");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(jLabel2)
                    .addComponent(jLabel1)
                    .addComponent(jLabel8)
                    .addComponent(jLabel9)
                    .addComponent(jLabel11)
                    .addComponent(jLabel13)
                    .addComponent(jLabel15)
                    .addComponent(jLabel16))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(timeQueued, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(totalTime, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(downloadTime, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(responseTime, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(url)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(responseLength)
                            .addComponent(responseCode)
                            .addComponent(requestType))
                        .addGap(0, 312, Short.MAX_VALUE))
                    .addComponent(requestTime, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(url, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(requestType))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(responseCode))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(responseLength))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(requestTime))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(responseTime))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(downloadTime))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(totalTime))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(timeQueued))
                .addContainerGap(126, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Basics", jPanel1);

        jLabel6.setText("Post Body");

        postBody.setColumns(20);
        postBody.setEditable(false);
        postBody.setRows(5);
        jScrollPane3.setViewportView(postBody);

        jLabel3.setText("Request Headers (Partial List!)");

        requestHeaders.setColumns(20);
        requestHeaders.setEditable(false);
        requestHeaders.setRows(5);
        jScrollPane4.setViewportView(requestHeaders);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)
                    .addComponent(jLabel6)
                    .addComponent(jLabel3))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 133, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 149, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Request", jPanel2);

        jLabel7.setText("Response Body");

        responseBody.setColumns(20);
        responseBody.setEditable(false);
        responseBody.setRows(5);
        jScrollPane2.setViewportView(responseBody);

        jLabel4.setText("Response Headers (Partial List)");

        responseHeaders.setColumns(20);
        responseHeaders.setRows(5);
        jScrollPane5.setViewportView(responseHeaders);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(41, 41, 41)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Response", jPanel3);

        jSplitPane1.setRightComponent(jTabbedPane1);

        add(jSplitPane1, java.awt.BorderLayout.CENTER);

        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jButton1.setText("Remove All");
        jButton1.addActionListener(formListener);
        jPanel4.add(jButton1);

        add(jPanel4, java.awt.BorderLayout.SOUTH);

    }

    // Code for dispatching events from components to event handlers.

    private class FormListener implements java.awt.event.ActionListener, java.awt.event.WindowListener, javax.swing.event.ListSelectionListener {
        FormListener() {}
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == jButton1) {
                NetworkMonitor.this.jButton1ActionPerformed(evt);
            }
        }

        public void windowActivated(java.awt.event.WindowEvent evt) {
        }

        public void windowClosed(java.awt.event.WindowEvent evt) {
        }

        public void windowClosing(java.awt.event.WindowEvent evt) {
            if (evt.getSource() == NetworkMonitor.this) {
                NetworkMonitor.this.windowClosing(evt);
            }
        }

        public void windowDeactivated(java.awt.event.WindowEvent evt) {
        }

        public void windowDeiconified(java.awt.event.WindowEvent evt) {
        }

        public void windowIconified(java.awt.event.WindowEvent evt) {
        }

        public void windowOpened(java.awt.event.WindowEvent evt) {
        }

        public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
            if (evt.getSource() == request) {
                NetworkMonitor.this.requestValueChanged(evt);
            }
        }
    }// </editor-fold>//GEN-END:initComponents

    private void windowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_windowClosing
        JavaSEPort.disableNetworkMonitor();
    }//GEN-LAST:event_windowClosing

    private void requestValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_requestValueChanged
        NetworkRequestObject nr = (NetworkRequestObject)request.getSelectedValue();
        if(nr != null) {
            responseLength.setText(nr.getContentLength());
            requestHeaders.setText(nr.getHeaders());
            requestType.setText(nr.getMethod());
            postBody.setText(nr.getRequestBody());
            responseBody.setText(nr.getResponseBody());
            responseCode.setText(nr.getResponseCode());
            responseHeaders.setText(nr.getResponseHeaders());
            url.setText(nr.getUrl());
            requestTime.setText("" + new Date(nr.getTimeSent())+" ("+nr.getTimeSent()+")");
            responseTime.setText("" + nr.getWaitTime()+"ms ("+nr.getTimeServerResponse()+")");
            downloadTime.setText("" + nr.getDownloadTime()+"ms ("+nr.getTimeComplete()+")");
            totalTime.setText("" + nr.getTotalTime()+"ms");
            timeQueued.setText("" + nr.getQueuedTime()+"ms ("+nr.getTimeQueued()+")");
        }
    }//GEN-LAST:event_requestValueChanged

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        
        DefaultListModel model = (DefaultListModel)request.getModel();
        model.removeAllElements();
    }//GEN-LAST:event_jButton1ActionPerformed

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel downloadTime;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea postBody;
    private javax.swing.JList request;
    private javax.swing.JTextArea requestHeaders;
    private javax.swing.JLabel requestTime;
    private javax.swing.JLabel requestType;
    private javax.swing.JTextArea responseBody;
    private javax.swing.JLabel responseCode;
    private javax.swing.JTextArea responseHeaders;
    private javax.swing.JLabel responseLength;
    private javax.swing.JLabel responseTime;
    private javax.swing.JLabel timeQueued;
    private javax.swing.JLabel totalTime;
    private javax.swing.JTextField url;
    // End of variables declaration//GEN-END:variables

    
}
