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

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Simulator window that drives {@link JavaSEMotionSensorManager}. The two
 * sliders set the simulated device orientation (which the accelerometer
 * reflects) while the buttons inject the transient motion needed to fire the
 * shake, flip, pick up and free fall gestures.
 *
 * @author Codename One
 */
public class MotionSimulation extends JFrame {
    private final JSlider pitchSlider = new JSlider(-90, 90, 0);
    private final JSlider rollSlider = new JSlider(-180, 180, 0);

    public MotionSimulation() {
        super("Motion / Gesture Simulation");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel sliders = new JPanel(new GridLayout(2, 1));
        sliders.setBorder(BorderFactory.createTitledBorder("Device orientation"));
        sliders.add(labeledSlider("Pitch (forward/back)", pitchSlider));
        sliders.add(labeledSlider("Roll (left/right)", rollSlider));

        ChangeListener orientationListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateOrientation();
            }
        };
        pitchSlider.addChangeListener(orientationListener);
        rollSlider.addChangeListener(orientationListener);

        JPanel buttons = new JPanel(new GridLayout(3, 2, 6, 6));
        buttons.setBorder(BorderFactory.createTitledBorder("Gestures"));
        buttons.add(gestureButton("Shake", new Runnable() {
            @Override
            public void run() {
                JavaSEMotionSensorManager.triggerShake();
            }
        }));
        buttons.add(gestureButton("Free Fall", new Runnable() {
            @Override
            public void run() {
                JavaSEMotionSensorManager.triggerFreeFall();
            }
        }));
        buttons.add(gestureButton("Flip Face Down", new Runnable() {
            @Override
            public void run() {
                pitchSlider.setValue(0);
                rollSlider.setValue(180);
            }
        }));
        buttons.add(gestureButton("Flip Face Up", new Runnable() {
            @Override
            public void run() {
                pitchSlider.setValue(0);
                rollSlider.setValue(0);
            }
        }));
        buttons.add(gestureButton("Pick Up", new Runnable() {
            @Override
            public void run() {
                JavaSEMotionSensorManager.triggerPickUp();
            }
        }));
        buttons.add(gestureButton("Reset Flat", new Runnable() {
            @Override
            public void run() {
                pitchSlider.setValue(0);
                rollSlider.setValue(0);
            }
        }));

        add(sliders, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationByPlatform(true);
        updateOrientation();
    }

    private JPanel labeledSlider(String title, JSlider slider) {
        JPanel p = new JPanel(new BorderLayout());
        slider.setMajorTickSpacing(45);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        p.add(new JLabel(title), BorderLayout.NORTH);
        p.add(slider, BorderLayout.CENTER);
        return p;
    }

    private JButton gestureButton(String text, final Runnable action) {
        JButton b = new JButton(text);
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
        return b;
    }

    private void updateOrientation() {
        JavaSEMotionSensorManager.setOrientation(pitchSlider.getValue(), rollSlider.getValue());
    }
}
