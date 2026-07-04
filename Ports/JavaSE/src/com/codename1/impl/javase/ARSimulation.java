/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.javase;

import com.codename1.ar.ARTrackingFailureReason;
import com.codename1.ar.ARTrackingMode;
import com.codename1.ar.ARTrackingState;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Simulator window that drives {@link JavaSEARImpl}, the simulated AR
 * backend. Lets developers exercise the failure and recognition paths that
 * are hard to hit on demand with real hardware: degraded tracking states,
 * changing light estimates, reference image detection and face appearance.
 * The virtual camera itself is driven directly on the AR view (mouse drag to
 * look, WASD / arrows to move, Q / E for up / down).
 *
 * @author Codename One
 */
public class ARSimulation extends JFrame {
    private final JComboBox<ARTrackingState> trackingState =
            new JComboBox<ARTrackingState>(ARTrackingState.values());
    private final JComboBox<ARTrackingFailureReason> failureReason =
            new JComboBox<ARTrackingFailureReason>(ARTrackingFailureReason.values());
    private final JSlider intensity = new JSlider(0, 200, 100);
    private final JSlider red = new JSlider(50, 150, 100);
    private final JSlider green = new JSlider(50, 150, 100);
    private final JSlider blue = new JSlider(50, 150, 100);

    public ARSimulation() {
        super("AR Simulation");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new GridLayout(3, 1));
        JLabel help = new JLabel("<html>Virtual camera: drag the AR view to look around,"
                + "<br>WASD / arrow keys move, Q / E go up / down."
                + "<br>Controls below act on the currently open AR session.</html>");
        help.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        top.add(help);

        JPanel tracking = new JPanel(new GridLayout(1, 3, 6, 6));
        tracking.setBorder(BorderFactory.createTitledBorder("Tracking state"));
        tracking.add(trackingState);
        tracking.add(failureReason);
        trackingState.setSelectedItem(ARTrackingState.TRACKING);
        failureReason.setSelectedItem(ARTrackingFailureReason.NONE);
        JButton applyTracking = new JButton("Apply");
        applyTracking.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JavaSEARImpl impl = requireSession();
                if (impl != null) {
                    impl.simSetTrackingState(
                            (ARTrackingState) trackingState.getSelectedItem(),
                            (ARTrackingFailureReason) failureReason.getSelectedItem());
                }
            }
        });
        tracking.add(applyTracking);
        top.add(tracking);

        JPanel actions = new JPanel(new GridLayout(1, 3, 6, 6));
        actions.setBorder(BorderFactory.createTitledBorder("Scene"));
        JButton redetect = new JButton("Re-detect planes");
        redetect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JavaSEARImpl impl = requireSession();
                if (impl != null) {
                    impl.simRedetectPlanes();
                }
            }
        });
        actions.add(redetect);
        JButton detectImage = new JButton("Detect reference image...");
        detectImage.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JavaSEARImpl impl = requireSession();
                if (impl == null) {
                    return;
                }
                String[] names = impl.simGetReferenceImageNames();
                if (names.length == 0) {
                    JOptionPane.showMessageDialog(ARSimulation.this,
                            "The open session registered no reference images.\n"
                            + "Pass them via ARSessionOptions.referenceImages(...).");
                    return;
                }
                String choice = (String) JOptionPane.showInputDialog(ARSimulation.this,
                        "Simulate the camera recognizing:", "Detect reference image",
                        JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
                if (choice != null) {
                    impl.simDetectImage(choice);
                }
            }
        });
        actions.add(detectImage);
        JButton toggleFace = new JButton("Toggle simulated face");
        toggleFace.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JavaSEARImpl impl = requireSession();
                if (impl == null) {
                    return;
                }
                if (impl.getOpenOptions().getTrackingMode() != ARTrackingMode.FACE) {
                    JOptionPane.showMessageDialog(ARSimulation.this,
                            "The open session is not in FACE tracking mode.\n"
                            + "Open it with ARSessionOptions.trackingMode(ARTrackingMode.FACE).");
                    return;
                }
                impl.simSetFaceVisible(!impl.simIsFaceVisible());
            }
        });
        actions.add(toggleFace);

        JPanel lightPanel = new JPanel(new GridLayout(4, 1));
        lightPanel.setBorder(BorderFactory.createTitledBorder(
                "Light estimate (100 = neutral)"));
        lightPanel.add(labeledSlider("Intensity", intensity));
        lightPanel.add(labeledSlider("Red", red));
        lightPanel.add(labeledSlider("Green", green));
        lightPanel.add(labeledSlider("Blue", blue));
        javax.swing.event.ChangeListener lightListener = new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                JavaSEARImpl impl = JavaSEARImpl.activeInstance;
                if (impl != null) {
                    impl.simSetLight(intensity.getValue() / 100f, red.getValue() / 100f,
                            green.getValue() / 100f, blue.getValue() / 100f);
                }
            }
        };
        intensity.addChangeListener(lightListener);
        red.addChangeListener(lightListener);
        green.addChangeListener(lightListener);
        blue.addChangeListener(lightListener);

        add(BorderLayout.NORTH, top);
        add(BorderLayout.CENTER, actions);
        add(BorderLayout.SOUTH, lightPanel);
        pack();
        setLocationByPlatform(true);
    }

    private JPanel labeledSlider(String label, JSlider slider) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.add(BorderLayout.WEST, new JLabel(label));
        p.add(BorderLayout.CENTER, slider);
        return p;
    }

    private JavaSEARImpl requireSession() {
        JavaSEARImpl impl = JavaSEARImpl.activeInstance;
        if (impl == null) {
            JOptionPane.showMessageDialog(this,
                    "No AR session is open. Open one in the app with AR.open(...).");
        }
        return impl;
    }
}
